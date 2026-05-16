package com.dianjinshou.modules.feishu.service;

import com.dianjinshou.common.exception.BusinessException;
import com.dianjinshou.common.security.SecurityUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.dianjinshou.modules.auth.entity.User;
import com.dianjinshou.modules.auth.mapper.UserMapper;
import com.dianjinshou.modules.feishu.util.DouyinUrlExtractor;
import com.dianjinshou.modules.streamer.dto.CreateStreamerRequest;
import com.dianjinshou.modules.streamer.service.StreamerService;
import com.dianjinshou.modules.streamer.vo.StreamerVO;
import com.lark.oapi.Client;
import com.lark.oapi.core.response.BaseResponse;
import com.lark.oapi.service.im.v1.model.CreateMessageReq;
import com.lark.oapi.service.im.v1.model.CreateMessageReqBody;
import com.lark.oapi.service.im.v1.model.CreateMessageResp;
import com.lark.oapi.service.im.v1.model.EventMessage;
import com.lark.oapi.service.im.v1.model.MentionEvent;
import com.lark.oapi.service.im.v1.model.P2MessageReceiveV1;
import com.lark.oapi.service.im.v1.model.P2MessageReceiveV1Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Processes incoming Feishu messages asynchronously. The sync entry {@link #submit} returns
 * immediately (Feishu enforces a 3-second event-ack deadline); the real work runs on a
 * pooled thread.
 */
@Component
public class FeishuMessageHandler {

    private static final Logger log = LoggerFactory.getLogger(FeishuMessageHandler.class);

    private final StreamerService streamerService;
    private final UserMapper userMapper;
    private final FeishuTaskDispatcher dispatcher;
    private final FeishuLarkClientFactory clientFactory;
    private final FeishuBotLauncher botLauncher;
    private final ObjectMapper jackson = new ObjectMapper();

    private final ExecutorService pool = Executors.newFixedThreadPool(4, new ThreadFactory() {
        final AtomicInteger n = new AtomicInteger(0);
        @Override public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "feishu-msg-" + n.incrementAndGet());
            t.setDaemon(true);
            return t;
        }
    });

    public FeishuMessageHandler(StreamerService streamerService,
                                UserMapper userMapper,
                                FeishuTaskDispatcher dispatcher,
                                FeishuLarkClientFactory clientFactory,
                                FeishuBotLauncher botLauncher) {
        this.streamerService = streamerService;
        this.userMapper = userMapper;
        this.dispatcher = dispatcher;
        this.clientFactory = clientFactory;
        this.botLauncher = botLauncher;
    }

    public void submit(Long userId, String appId, P2MessageReceiveV1 event) {
        pool.submit(() -> {
            try {
                handle(userId, appId, event);
            } catch (Throwable t) {
                log.error("Feishu message handler crashed: userId={} appId={}", userId, appId, t);
            }
        });
    }

    private void handle(Long userId, String appId, P2MessageReceiveV1 event) {
        P2MessageReceiveV1Data data = event.getEvent();
        if (data == null || data.getMessage() == null) return;
        EventMessage msg = data.getMessage();
        String chatId = msg.getChatId();
        String chatType = msg.getChatType(); // "p2p" | "group"
        String content = msg.getContent();
        String text = extractPlainText(content);
        log.info("Feishu msg: appId={} chatType={} chatId={} text={}", appId, chatType, chatId, text);

        // 群聊必须 @ 到当前机器人才处理；未 @ 或 @ 了别的 bot 直接忽略。
        // 私聊（p2p）无需 @。
        if ("group".equalsIgnoreCase(chatType)) {
            String myOpenId = botLauncher.getBotOpenId(appId);
            if (myOpenId == null || myOpenId.isEmpty()) {
                log.warn("Feishu msg ignored: appId={} 没有缓存到本 bot 的 open_id（可能启动时拉取失败）", appId);
                return;
            }
            if (!mentionsInclude(msg.getMentions(), myOpenId)) {
                log.info("Feishu msg ignored: appId={} 未被 @，忽略（群里的其他 bot 会处理）", appId);
                return;
            }
        }

        List<DouyinUrlExtractor.Result> targets = DouyinUrlExtractor.extractAll(text);
        if (targets.isEmpty()) {
            reply(appId, chatId, "请发送一个或多个抖音直播间链接/抖音号。\n"
                    + "批量示例：88668880097,53802526428,xueliyingyu （用逗号/空格/顿号/换行分隔均可）");
            return;
        }

        // 同一用户的 SecurityContext 设置一次就够 —— 批量里后续 create 共享
        User user = userMapper.selectById(userId);
        if (user == null) {
            reply(appId, chatId, "❌ 机器人绑定的用户不存在");
            return;
        }

        boolean batch = targets.size() > 1;
        List<String> added = new ArrayList<>();   // "主播名 (accountId)"
        List<String> skipped = new ArrayList<>(); // "主播名 - 原因"
        List<String> failed = new ArrayList<>();  // "输入值 - 原因"

        int idx = 0;
        for (DouyinUrlExtractor.Result target : targets) {
            // 批量时每次调 douyin API 间隔 2.5s，避免同一 IP 短时间内大量请求触发反爬
            // 第一个不等；首轮失败已避免就不浪费时间
            if (batch && idx++ > 0) {
                try { Thread.sleep(2500); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
            }

            String label = target.value;

            Map<String, Object> payload = new HashMap<>();
            payload.put("kind", target.type);
            payload.put("value", target.value);
            FeishuTaskDispatcher.TaskResult result = dispatcher.dispatch(userId, "resolve_douyin", payload);

            if (!result.ok) {
                if ("timeout".equals(result.error)) {
                    // 桌面不在线 —— 整条批量都解析不了，直接回并终止
                    reply(appId, chatId, "❌ 点金手桌面客户端未在线，无法解析抖音链接。请打开客户端后重试。");
                    return;
                }
                failed.add(label + " - " + (result.error != null ? result.error : "未知错误"));
                continue;
            }

            Map<String, Object> d = result.data != null ? result.data : Collections.emptyMap();
            String accountId = str(d.get("accountId"));
            String secUid = str(d.get("secUid"));
            String anchorName = str(d.get("anchorName"));
            String avatar = str(d.get("anchorAvatar"));
            if (accountId == null || accountId.isEmpty()) {
                failed.add(label + " - 无法识别抖音账号");
                continue;
            }

            CreateStreamerRequest req = new CreateStreamerRequest();
            req.setPlatform("douyin");
            req.setAccountId(accountId);
            req.setAnchorName(anchorName);
            req.setAnchorAvatar(avatar);
            req.setSecUid(secUid);
            req.setAccountType("own");

            try {
                StreamerVO vo = withUser(user, () -> streamerService.create(req));
                added.add(safe(vo.getAnchorName(), accountId) + " (" + accountId + ")");
            } catch (BusinessException be) {
                skipped.add(safe(anchorName, accountId) + " - " + (be.getMessage() != null ? be.getMessage() : "添加失败"));
            } catch (Exception e) {
                log.error("Create streamer failed: {}", label, e);
                failed.add(label + " - " + e.getMessage());
            }
        }

        reply(appId, chatId, renderSummary(batch, targets.size(), added, skipped, failed));
    }

    private String renderSummary(boolean batch, int total,
                                 List<String> added, List<String> skipped, List<String> failed) {
        if (!batch) {
            // 单个目标 —— 保留之前的简短反馈体验
            if (!added.isEmpty()) return "✅ 已添加「" + added.get(0) + "」到你的直播间列表";
            if (!skipped.isEmpty()) return "ℹ️ " + skipped.get(0);
            if (!failed.isEmpty()) return "❌ " + failed.get(0);
            return "处理完成";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("批量添加完成：共 ").append(total).append(" 个")
          .append(" | 成功 ").append(added.size())
          .append(" | 已存在 ").append(skipped.size())
          .append(" | 失败 ").append(failed.size());
        if (!added.isEmpty()) {
            sb.append("\n\n✅ 已添加 (").append(added.size()).append(")：");
            for (String s : added) sb.append("\n• ").append(s);
        }
        if (!skipped.isEmpty()) {
            sb.append("\n\nℹ️ 跳过 (").append(skipped.size()).append(")：");
            for (String s : skipped) sb.append("\n• ").append(s);
        }
        if (!failed.isEmpty()) {
            sb.append("\n\n❌ 失败 (").append(failed.size()).append(")：");
            for (String s : failed) sb.append("\n• ").append(s);
        }
        return sb.toString();
    }

    private boolean mentionsInclude(MentionEvent[] mentions, String myOpenId) {
        if (mentions == null || mentions.length == 0) return false;
        for (MentionEvent m : mentions) {
            if (m == null || m.getId() == null) continue;
            if (myOpenId.equals(m.getId().getOpenId())) return true;
        }
        return false;
    }

    private <T> T withUser(User user, java.util.function.Supplier<T> action) {
        SecurityUser principal = new SecurityUser(user.getId(), user.getRole(), user.getOrgId());
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);
        try {
            return action.get();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    /**
     * message.content is a JSON string like {"text":"hello @_user_1 world"}. We only care about text;
     * a tiny manual extraction avoids pulling in another JSON lib just for this.
     */
    private String extractPlainText(String contentJson) {
        if (contentJson == null) return "";
        // The JSON "text" field — escaped \n, \", etc. are acceptable.
        int i = contentJson.indexOf("\"text\"");
        if (i < 0) return contentJson;
        int colon = contentJson.indexOf(':', i);
        if (colon < 0) return contentJson;
        int qStart = contentJson.indexOf('"', colon);
        if (qStart < 0) return contentJson;
        StringBuilder sb = new StringBuilder();
        for (int p = qStart + 1; p < contentJson.length(); p++) {
            char c = contentJson.charAt(p);
            if (c == '\\' && p + 1 < contentJson.length()) {
                char n = contentJson.charAt(p + 1);
                if (n == 'n') sb.append('\n');
                else if (n == 't') sb.append('\t');
                else sb.append(n);
                p++;
            } else if (c == '"') {
                return sb.toString();
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private void reply(String appId, String chatId, String text) {
        try {
            Client client = clientFactory.get(appId);
            if (client == null) {
                log.warn("No Feishu client for appId={}, cannot reply", appId);
                return;
            }
            // 用 Jackson 正规序列化 —— 批量汇总里有换行/引号/控制字符，手拼 JSON 会被飞书 230001 拒掉
            String content = jackson.writeValueAsString(Collections.singletonMap("text", text));
            CreateMessageReq req = CreateMessageReq.newBuilder()
                    .receiveIdType("chat_id")
                    .createMessageReqBody(CreateMessageReqBody.newBuilder()
                            .receiveId(chatId)
                            .msgType("text")
                            .content(content)
                            .build())
                    .build();
            CreateMessageResp resp = client.im().message().create(req);
            if (!resp.success()) {
                log.warn("Feishu reply failed: code={} msg={}", resp.getCode(), resp.getMsg());
            }
        } catch (Exception e) {
            log.error("Failed to reply on Feishu", e);
        }
    }

    private static String str(Object o) { return o == null ? null : o.toString(); }
    private static String safe(String v, String def) { return (v == null || v.isEmpty()) ? def : v; }
}
