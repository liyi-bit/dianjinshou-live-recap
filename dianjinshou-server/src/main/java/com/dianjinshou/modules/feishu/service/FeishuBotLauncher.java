package com.dianjinshou.modules.feishu.service;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.dianjinshou.modules.feishu.entity.FeishuBot;
import com.dianjinshou.modules.feishu.mapper.FeishuBotMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lark.oapi.event.EventDispatcher;
import com.lark.oapi.service.im.ImService;
import com.lark.oapi.service.im.v1.model.P2MessageReceiveV1;
import com.lark.oapi.ws.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Maintains one outbound WebSocket long connection per enabled Feishu bot.
 *
 * <p>Feishu limits event handlers to 3 seconds and will re-send the event on timeout — therefore
 * the handler submits to an executor and returns immediately. The real work (URL parsing +
 * streamer creation + reply) runs asynchronously in {@link FeishuMessageHandler}.
 */
@Component
public class FeishuBotLauncher {

    private static final Logger log = LoggerFactory.getLogger(FeishuBotLauncher.class);

    private final FeishuBotMapper botMapper;
    private final FeishuMessageHandler messageHandler;
    private final FeishuLarkClientFactory clientFactory;

    private final Map<String, Client> wsClients = new ConcurrentHashMap<>();
    // appId -> bot 自己的 open_id，用于判断群消息是否真的 @ 了我
    private final Map<String, String> botOpenIds = new ConcurrentHashMap<>();
    private final ObjectMapper jackson = new ObjectMapper();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1, r -> {
        Thread t = new Thread(r, "feishu-bot-launcher");
        t.setDaemon(true);
        return t;
    });

    /** 查询某个 bot 的 open_id（启动时从 /bot/v3/info 拿到并缓存），未缓存返回 null。 */
    public String getBotOpenId(String appId) {
        return botOpenIds.get(appId);
    }

    public FeishuBotLauncher(FeishuBotMapper botMapper,
                             @Lazy FeishuMessageHandler messageHandler,
                             FeishuLarkClientFactory clientFactory) {
        this.botMapper = botMapper;
        this.messageHandler = messageHandler;
        this.clientFactory = clientFactory;
    }

    @PostConstruct
    public void startAll() {
        // Delay slightly so all beans (especially Mybatis) are ready.
        scheduler.schedule(() -> {
            try {
                for (FeishuBot bot : botMapper.selectList(null)) {
                    if (bot.getStatus() != null && bot.getStatus() == 1) {
                        startOne(bot);
                    }
                }
            } catch (Exception e) {
                log.error("Failed to start Feishu bots on boot", e);
            }
        }, 2, TimeUnit.SECONDS);
    }

    @PreDestroy
    public void stopAll() {
        for (String appId : wsClients.keySet()) {
            stopOne(appId);
        }
        scheduler.shutdownNow();
    }

    public void startOne(FeishuBot bot) {
        final String appId = bot.getAppId();
        final String appSecret = bot.getAppSecret();
        stopOne(appId); // idempotent

        EventDispatcher dispatcher = EventDispatcher.newBuilder("", "")
                .onP2MessageReceiveV1(new ImService.P2MessageReceiveV1Handler() {
                    @Override
                    public void handle(P2MessageReceiveV1 event) {
                        messageHandler.submit(bot.getUserId(), appId, event);
                    }
                })
                .build();

        Client wsClient = new Client.Builder(appId, appSecret)
                .eventHandler(dispatcher)
                .build();

        try {
            wsClient.start();
            wsClients.put(appId, wsClient);
            // Pre-warm the REST client so replies don't pay init cost on first use.
            clientFactory.getOrCreate(appId, appSecret);
            // 拿 bot 自己的 open_id 并缓存 — 用于消息接收时判断 @ 的是不是我
            try {
                String openId = fetchBotOpenId(appId, appSecret);
                if (openId != null) {
                    botOpenIds.put(appId, openId);
                    log.info("Feishu bot open_id cached: appId={} openId={}", appId, openId);
                }
            } catch (Exception e) {
                log.warn("Failed to fetch bot open_id for {}: {}", appId, e.getMessage());
            }
            // 写回连接时间 — UI 据此显示"已连接"，同时清掉上次的 lastError
            UpdateWrapper<FeishuBot> uw = new UpdateWrapper<>();
            uw.eq("app_id", appId).eq("deleted", 0)
                    .set("last_connected_at", LocalDateTime.now())
                    .set("last_error", null);
            botMapper.update(null, uw);
            log.info("Feishu bot started: appId={} user={}", appId, bot.getUserId());
        } catch (Exception e) {
            log.error("Feishu bot failed to start: appId={} err={}", appId, e.getMessage(), e);
            UpdateWrapper<FeishuBot> uw = new UpdateWrapper<>();
            String err = e.getMessage();
            if (err != null && err.length() > 250) err = err.substring(0, 250);
            uw.eq("app_id", appId).eq("deleted", 0)
                    .set("last_error", err);
            botMapper.update(null, uw);
        }
    }

    public void stopOne(String appId) {
        Client client = wsClients.remove(appId);
        botOpenIds.remove(appId);
        if (client != null) {
            try {
                // com.lark.oapi.ws.Client may not expose a close/stop; rely on GC when reference dropped.
                // If the SDK exposes a disconnect method in a future version, call it here.
                // For now, dropping the reference is sufficient for hot-reload.
            } catch (Exception ignored) {
            }
            log.info("Feishu bot stopped: appId={}", appId);
        }
        clientFactory.evict(appId);
    }

    /** 通过飞书 REST API 拿到 bot 自己的 open_id。SDK 未提供该接口，手写 HTTP 调用。 */
    private String fetchBotOpenId(String appId, String appSecret) throws Exception {
        // 1. 拿 tenant_access_token
        String tokenBody = jackson.writeValueAsString(new java.util.HashMap<String, String>() {{
            put("app_id", appId);
            put("app_secret", appSecret);
        }});
        JsonNode tokenResp = httpJson(
                "POST",
                "https://open.feishu.cn/open-apis/auth/v3/tenant_access_token/internal",
                null, tokenBody);
        if (tokenResp == null || tokenResp.path("code").asInt(-1) != 0) {
            return null;
        }
        String token = tokenResp.path("tenant_access_token").asText();
        if (token.isEmpty()) return null;

        // 2. 拿 bot 的 open_id
        JsonNode botResp = httpJson("GET", "https://open.feishu.cn/open-apis/bot/v3/info", token, null);
        if (botResp == null || botResp.path("code").asInt(-1) != 0) {
            return null;
        }
        String openId = botResp.path("bot").path("open_id").asText();
        return openId.isEmpty() ? null : openId;
    }

    private JsonNode httpJson(String method, String url, String bearer, String body) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod(method);
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(8000);
        if (bearer != null) conn.setRequestProperty("Authorization", "Bearer " + bearer);
        if (body != null) {
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            conn.setDoOutput(true);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }
        }
        int status = conn.getResponseCode();
        InputStream is = status >= 400 ? conn.getErrorStream() : conn.getInputStream();
        if (is == null) return null;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        while ((n = is.read(buf)) > 0) baos.write(buf, 0, n);
        return jackson.readTree(baos.toByteArray());
    }
}
