package com.dianjinshou.integration.ai;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.dianjinshou.modules.admin.service.ThirdPartySettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * 云雾AI客户端 - OpenAI兼容接口，支持大模型切换。
 * 凭据/端点/模型每次请求时从 {@link ThirdPartySettings} 动态读取，
 * 后台修改 system_settings 后立即生效，无需重启。
 */
public class YunwuAiClient implements AiAnalysisClient {

    private static final Logger log = LoggerFactory.getLogger(YunwuAiClient.class);

    private static final int MAX_RETRIES = 2;
    private static final int TIMEOUT_MS = 120_000;

    private final ThirdPartySettings settings;

    private String apiKey(Long userId)   { return settings.getYunwuApiKey(userId); }
    private String endpoint(Long userId) { return settings.getYunwuEndpoint(userId); }
    private String model(Long userId)    { return settings.getYunwuModel(userId); }

    private static final String SYSTEM_PROMPT =
            "你是一个专业的直播复盘分析师AI。请严格按照以下JSON格式返回分析结果，不要包含任何多余文字或markdown代码块标记：\n" +
            "{\n" +
            "  \"dimensions\": {\n" +
            "    \"opening\": {\"score\": 0-100, \"comment\": \"评语\"},\n" +
            "    \"retention\": {\"score\": 0-100, \"comment\": \"评语\"},\n" +
            "    \"product\": {\"score\": 0-100, \"comment\": \"评语\"},\n" +
            "    \"promotion\": {\"score\": 0-100, \"comment\": \"评语\"},\n" +
            "    \"interaction\": {\"score\": 0-100, \"comment\": \"评语\"},\n" +
            "    \"expression\": {\"score\": 0-100, \"comment\": \"评语\"},\n" +
            "    \"compliance\": {\"score\": 0-100, \"comment\": \"评语\"},\n" +
            "    \"fanClub\": {\"score\": 0-100, \"comment\": \"评语\"},\n" +
            "    \"privateDomain\": {\"score\": 0-100, \"comment\": \"评语\"},\n" +
            "    \"persona\": {\"score\": 0-100, \"comment\": \"评语\"},\n" +
            "    \"closing\": {\"score\": 0-100, \"comment\": \"评语\"},\n" +
            "    \"overall\": {\"score\": 0-100, \"comment\": \"评语\"}\n" +
            "  },\n" +
            "  \"diagnosis\": {\n" +
            "    \"strengths\": [\"优势1\", \"优势2\"],\n" +
            "    \"weaknesses\": [\"不足1\", \"不足2\"],\n" +
            "    \"suggestions\": [\"建议1\", \"建议2\"]\n" +
            "  },\n" +
            "  \"keywords\": {\n" +
            "    \"operational\": {\"类别名\": [\"关键词1\", \"关键词2\"]},\n" +
            "    \"sensitive\": {\"类别名\": [\"敏感词1\"]}\n" +
            "  },\n" +
            "  \"compass\": {\n" +
            "    \"contentRatio\": {\n" +
            "      \"score\": 0-100,\n" +
            "      \"items\": [\n" +
            "        {\"name\": \"话题话术\", \"percent\": 0.0-100.0},\n" +
            "        {\"name\": \"行动指令\", \"percent\": 0.0-100.0},\n" +
            "        {\"name\": \"留人话术\", \"percent\": 0.0-100.0},\n" +
            "        {\"name\": \"塑品话术\", \"percent\": 0.0-100.0},\n" +
            "        {\"name\": \"互动话术\", \"percent\": 0.0-100.0},\n" +
            "        {\"name\": \"促单话术\", \"percent\": 0.0-100.0},\n" +
            "        {\"name\": \"逼单话术\", \"percent\": 0.0-100.0},\n" +
            "        {\"name\": \"福利话术\", \"percent\": 0.0-100.0},\n" +
            "        {\"name\": \"开场话术\", \"percent\": 0.0-100.0},\n" +
            "        {\"name\": \"人设话术\", \"percent\": 0.0-100.0}\n" +
            "      ]\n" +
            "    },\n" +
            "    \"liveRhythm\": {\n" +
            "      \"score\": 0-100,\n" +
            "      \"items\": [\n" +
            "        {\"name\": \"开场暖场\", \"percent\": 0.0-100.0},\n" +
            "        {\"name\": \"产品讲解\", \"percent\": 0.0-100.0},\n" +
            "        {\"name\": \"互动环节\", \"percent\": 0.0-100.0},\n" +
            "        {\"name\": \"促单逼单\", \"percent\": 0.0-100.0},\n" +
            "        {\"name\": \"福利发放\", \"percent\": 0.0-100.0},\n" +
            "        {\"name\": \"过渡衔接\", \"percent\": 0.0-100.0},\n" +
            "        {\"name\": \"闲聊休息\", \"percent\": 0.0-100.0}\n" +
            "      ]\n" +
            "    }\n" +
            "  },\n" +
            "  \"summary\": \"一段简洁的总结性文字，100字以内\"\n" +
            "}";

    public YunwuAiClient(ThirdPartySettings settings) {
        this.settings = settings;
        log.info("YunwuAiClient initialized (dynamic config via ThirdPartySettings)");
    }

    @Override
    public AiAnalysisResult analyze(Long userId, String asrText, String industry, String promptTemplate) {
        log.info("Yunwu AI analyzing: user={}, industry={}, textLength={}, model={}",
                userId, industry, asrText != null ? asrText.length() : 0, model(userId));

        Exception lastException = null;
        for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
            try {
                return doAnalyze(userId, promptTemplate);
            } catch (Exception e) {
                lastException = e;
                log.warn("Yunwu AI attempt {}/{} failed: {}", attempt + 1, MAX_RETRIES + 1, e.getMessage());
                if (attempt < MAX_RETRIES) {
                    try {
                        Thread.sleep(1000L * (attempt + 1));
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        throw new RuntimeException("云雾AI调用失败，已重试" + MAX_RETRIES + "次: " +
                (lastException != null ? lastException.getMessage() : "未知错误"), lastException);
    }

    private AiAnalysisResult doAnalyze(Long userId, String promptTemplate) {
        JSONObject body = new JSONObject();
        body.set("model", model(userId));
        body.set("temperature", 0.7);
        body.set("max_tokens", 4096);

        JSONArray messages = new JSONArray();
        messages.add(new JSONObject().set("role", "system").set("content", SYSTEM_PROMPT));
        messages.add(new JSONObject().set("role", "user").set("content", promptTemplate));
        body.set("messages", messages);

        String ep = endpoint(userId);
        String url = ep.endsWith("/") ? ep + "chat/completions" : ep + "/chat/completions";

        log.debug("Calling Yunwu AI: url={}, model={}", url, model(userId));

        HttpResponse response = HttpRequest.post(url)
                .header("Authorization", "Bearer " + apiKey(userId))
                .header("Content-Type", "application/json")
                .body(body.toString())
                .timeout(TIMEOUT_MS)
                .execute();

        if (!response.isOk()) {
            throw new RuntimeException("云雾AI HTTP " + response.getStatus() + ": " + response.body());
        }

        String responseBody = response.body();
        JSONObject json = JSONUtil.parseObj(responseBody);

        JSONArray choices = json.getJSONArray("choices");
        if (choices == null || choices.isEmpty()) {
            throw new RuntimeException("云雾AI返回空结果");
        }

        String content = choices.getJSONObject(0)
                .getJSONObject("message")
                .getStr("content", "");

        long totalTokens = 0;
        JSONObject usage = json.getJSONObject("usage");
        if (usage != null) {
            totalTokens = usage.getLong("total_tokens", 0L);
        }

        log.info("Yunwu AI response received: contentLength={}, totalTokens={}", content.length(), totalTokens);
        return parseAiResponse(content, totalTokens);
    }

    private AiAnalysisResult parseAiResponse(String content, long totalTokens) {
        AiAnalysisResult result = new AiAnalysisResult();
        result.setConsumedChars(totalTokens);

        String jsonStr = extractJson(content);

        try {
            JSONObject parsed = JSONUtil.parseObj(jsonStr);

            JSONObject dimensions = parsed.getJSONObject("dimensions");
            if (dimensions != null) {
                result.setAiResult(new JSONObject().set("dimensions", dimensions).toString());
            }

            JSONObject diagnosis = parsed.getJSONObject("diagnosis");
            if (diagnosis != null) {
                result.setAiDiagnosis(diagnosis.toString());
            }

            JSONObject keywords = parsed.getJSONObject("keywords");
            if (keywords != null) {
                result.setKeywordSummary(keywords.toString());
            }

            JSONObject compass = parsed.getJSONObject("compass");
            if (compass != null) {
                result.setContentCompass(compass.toString());
            }

            String summary = parsed.getStr("summary");
            if (summary != null) {
                result.setSummary(summary);
            }
        } catch (Exception e) {
            log.warn("Failed to parse structured JSON, storing raw content: {}", e.getMessage());
            result.setAiResult(content);
            result.setSummary(content.length() > 500 ? content.substring(0, 500) : content);
        }

        return result;
    }

    private static final String OPTIMIZE_SYSTEM_PROMPT =
            "你是一个专业的直播话术编辑。你的任务是优化直播转写文本，使其更加通顺、专业。\n" +
            "优化规则：\n" +
            "1. 去除口语化的语气词和填充词（如：嗯、啊、那个、就是说、然后呢、对吧）\n" +
            "2. 修正标点符号，添加适当的逗号、句号、感叹号\n" +
            "3. 修正明显的语音识别错误（如同音字错误）\n" +
            "4. 优化句子结构，使表达更简洁流畅\n" +
            "5. 按话术段落合理分段（用换行分隔不同话题）\n" +
            "6. 保留原文的核心意思和风格，不要改变内容本身\n" +
            "7. 保留直播特有的表达方式（如互动话术、促单话术）\n\n" +
            "直接返回优化后的文本，不要包含任何解释或标记。";

    @Override
    public String optimizeText(Long userId, String asrText) {
        log.info("Yunwu AI optimizing text: user={}, length={}",
                userId, asrText != null ? asrText.length() : 0);

        // Truncate very long text to avoid token limits
        String text = asrText;
        if (text != null && text.length() > 15000) {
            text = text.substring(0, 15000) + "\n\n[文本过长，已截取前15000字]";
        }

        JSONObject body = new JSONObject();
        body.set("model", model(userId));
        body.set("temperature", 0.3);
        body.set("max_tokens", 8192);

        JSONArray messages = new JSONArray();
        messages.add(new JSONObject().set("role", "system").set("content", OPTIMIZE_SYSTEM_PROMPT));
        messages.add(new JSONObject().set("role", "user").set("content",
                "请优化以下直播转写文本：\n\n" + text));
        body.set("messages", messages);

        String ep = endpoint(userId);
        String url = ep.endsWith("/") ? ep + "chat/completions" : ep + "/chat/completions";

        HttpResponse response = HttpRequest.post(url)
                .header("Authorization", "Bearer " + apiKey(userId))
                .header("Content-Type", "application/json")
                .body(body.toString())
                .timeout(TIMEOUT_MS)
                .execute();

        if (!response.isOk()) {
            throw new RuntimeException("优化文本失败 HTTP " + response.getStatus());
        }

        JSONObject json = JSONUtil.parseObj(response.body());
        JSONArray choices = json.getJSONArray("choices");
        if (choices == null || choices.isEmpty()) {
            throw new RuntimeException("AI返回空结果");
        }

        String content = choices.getJSONObject(0)
                .getJSONObject("message")
                .getStr("content", "");

        // Remove potential markdown wrapping
        String trimmed = content.trim();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            if (firstNewline > 0) trimmed = trimmed.substring(firstNewline + 1);
            if (trimmed.endsWith("```")) trimmed = trimmed.substring(0, trimmed.length() - 3).trim();
        }

        log.info("Text optimization complete: inputLen={}, outputLen={}",
                asrText != null ? asrText.length() : 0, trimmed.length());
        return trimmed;
    }

    private static final String CLASSIFY_SYSTEM_PROMPT =
            "你是一个专业的直播话术分类专家。你的任务是将每一段直播话术分类到以下10个类别之一：\n" +
            "话题话术、行动指令、留人话术、塑品话术、互动话术、促单话术、逼单话术、福利话术、开场话术、人设话术\n\n" +
            "分类标准：\n" +
            "- 开场话术：开场问候、暖场、介绍直播主题\n" +
            "- 留人话术：挽留观众停留、吸引新观众、预告后续内容\n" +
            "- 塑品话术：产品介绍、材质描述、功效说明、品牌故事\n" +
            "- 促单话术：引导下单、限时优惠、价格对比、购买引导\n" +
            "- 逼单话术：倒计时、库存紧张、最后机会、催促下单\n" +
            "- 互动话术：引导点赞关注评论、问答互动、粉丝团引导\n" +
            "- 福利话术：抽奖、赠品、红包、专属优惠\n" +
            "- 行动指令：明确要求观众执行某个动作\n" +
            "- 人设话术：个人经历、专业背景、情感分享、价值观输出\n" +
            "- 话题话术：闲聊、时事、故事、与产品无直接关系的话题\n\n" +
            "请严格按照JSON数组格式返回，数组中每个元素是对应段落的类别名称（字符串），数组长度必须与输入段落数量一致。\n" +
            "示例：[\"开场话术\",\"塑品话术\",\"促单话术\"]\n" +
            "不要包含任何多余文字或markdown标记。";

    @Override
    public List<String> classifyParagraphs(Long userId, List<String> paragraphTexts) {
        if (paragraphTexts == null || paragraphTexts.isEmpty()) {
            return new ArrayList<>();
        }
        log.info("Yunwu AI classifying {} paragraphs, user={}, model={}",
                paragraphTexts.size(), userId, model(userId));

        StringBuilder userContent = new StringBuilder();
        userContent.append("请对以下").append(paragraphTexts.size()).append("段直播话术进行分类：\n\n");
        for (int i = 0; i < paragraphTexts.size(); i++) {
            userContent.append("【段落").append(i + 1).append("】").append(paragraphTexts.get(i)).append("\n");
        }

        JSONObject body = new JSONObject();
        body.set("model", model(userId));
        body.set("temperature", 0.3);
        body.set("max_tokens", 4096);

        JSONArray messages = new JSONArray();
        messages.add(new JSONObject().set("role", "system").set("content", CLASSIFY_SYSTEM_PROMPT));
        messages.add(new JSONObject().set("role", "user").set("content", userContent.toString()));
        body.set("messages", messages);

        String ep = endpoint(userId);
        String url = ep.endsWith("/") ? ep + "chat/completions" : ep + "/chat/completions";

        try {
            HttpResponse response = HttpRequest.post(url)
                    .header("Authorization", "Bearer " + apiKey(userId))
                    .header("Content-Type", "application/json")
                    .body(body.toString())
                    .timeout(TIMEOUT_MS)
                    .execute();

            if (!response.isOk()) {
                log.warn("Classify paragraphs HTTP {}: {}", response.getStatus(), response.body());
                return null;
            }

            JSONObject json = JSONUtil.parseObj(response.body());
            JSONArray choices = json.getJSONArray("choices");
            if (choices == null || choices.isEmpty()) {
                log.warn("Classify paragraphs: empty choices");
                return null;
            }

            String content = choices.getJSONObject(0)
                    .getJSONObject("message")
                    .getStr("content", "");

            String jsonStr = extractJson(content);
            JSONArray arr = JSONUtil.parseArray(jsonStr);

            List<String> results = new ArrayList<>();
            for (int i = 0; i < arr.size() && i < paragraphTexts.size(); i++) {
                results.add(arr.getStr(i, "话题话术"));
            }
            // Pad if AI returned fewer items
            while (results.size() < paragraphTexts.size()) {
                results.add("话题话术");
            }

            log.info("Paragraph classification complete: {} paragraphs classified", results.size());
            return results;
        } catch (Exception e) {
            log.warn("Failed to classify paragraphs: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 从AI响应中提取JSON，去除可能的markdown代码块标记
     */
    private String extractJson(String content) {
        String trimmed = content.trim();
        if (trimmed.startsWith("```json")) {
            trimmed = trimmed.substring(7);
        } else if (trimmed.startsWith("```")) {
            trimmed = trimmed.substring(3);
        }
        if (trimmed.endsWith("```")) {
            trimmed = trimmed.substring(0, trimmed.length() - 3);
        }
        return trimmed.trim();
    }
}
