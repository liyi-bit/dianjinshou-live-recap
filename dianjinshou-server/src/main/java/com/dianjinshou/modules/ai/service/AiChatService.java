package com.dianjinshou.modules.ai.service;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dianjinshou.common.exception.BusinessException;
import com.dianjinshou.common.response.ErrorCode;
import com.dianjinshou.common.response.PageResult;
import com.dianjinshou.common.security.SecurityContextHelper;
import com.dianjinshou.integration.ai.AiConfig;
import com.dianjinshou.modules.admin.service.DailyAiQuotaService;
import com.dianjinshou.modules.admin.service.ThirdPartySettings;
import com.dianjinshou.modules.ai.dto.ChatRequest;
import com.dianjinshou.modules.ai.entity.AiConversation;
import com.dianjinshou.modules.ai.mapper.AiConversationMapper;
import com.dianjinshou.modules.ai.vo.ChatMessageVO;
import com.dianjinshou.modules.ai.vo.PresetQuestionVO;
import com.dianjinshou.modules.comparison.entity.Comparison;
import com.dianjinshou.modules.comparison.mapper.ComparisonMapper;
import com.dianjinshou.modules.recap.entity.AnalysisTask;
import com.dianjinshou.modules.recap.mapper.AnalysisTaskMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AiChatService {

    private static final Logger log = LoggerFactory.getLogger(AiChatService.class);
    private static final int MAX_HISTORY_ROUNDS = 50;
    private static final int CONTEXT_ROUNDS = 10;
    private static final int API_TIMEOUT_MS = 120_000;
    private static final Map<String, List<PresetQuestionVO>> PRESETS = new HashMap<>();

    /** 前端模型ID → 云雾API模型名称 */
    private static final Map<String, String> MODEL_MAP = new HashMap<>();
    static {
        MODEL_MAP.put("deepseek_r1", "deepseek-reasoner");
        MODEL_MAP.put("deepseek_v3", "deepseek-chat");
        MODEL_MAP.put("qwen_max", "qwen-max");
        MODEL_MAP.put("qwen_plus", "qwen-plus");
        MODEL_MAP.put("doubao_pro", "doubao-seed-2-0-pro-260215");
        MODEL_MAP.put("doubao_lite", "doubao-seed-2-0-lite-260215");
        MODEL_MAP.put("gpt4o_mini", "gpt-4o-mini");
        MODEL_MAP.put("glm4", "glm-4-flash");
        MODEL_MAP.put("claude_sonnet", "claude-sonnet-4-20250514");
        MODEL_MAP.put("moonshot", "moonshot-v1-auto");
    }

    /** 各助手类型的 system prompt */
    private static final Map<String, String> SYSTEM_PROMPTS = new HashMap<>();
    static {
        SYSTEM_PROMPTS.put("operation",
                "你是「点金手」平台的AI运营助手，专精直播电商运营分析。\n" +
                "你的能力包括：分析直播框架和风格、用户画像分析、留人策略拆解、互动手段分析、粉团运营优化、营销塑品方式分析、私域导流分析、人设塑造拆解等。\n" +
                "回复要求：\n" +
                "1. 使用Markdown格式，结构清晰，分点列出\n" +
                "2. 给出具体可执行的建议，而非泛泛而谈\n" +
                "3. 结合抖音直播电商实战经验回答\n" +
                "4. 适当使用表格、列表等格式提升可读性\n" +
                "5. 回答简洁专业，控制在500字以内");

        SYSTEM_PROMPTS.put("compliance",
                "你是「点金手」平台的AI违规助手，专精直播合规检测与风险分析。\n" +
                "你的能力包括：敏感词检测、虚假宣传识别、绝对化用语排查、场外交易引导检测、涉政涉黄内容排查、商品描述合规检查、评论区违规分析。\n" +
                "回复要求：\n" +
                "1. 使用Markdown格式，结构清晰\n" +
                "2. 明确标注风险等级（高/中/低）\n" +
                "3. 引用相关法律法规（如《广告法》、《电商法》、平台规则）\n" +
                "4. 给出具体的合规替换建议\n" +
                "5. 使用表格对比违规表述和合规替换方案\n" +
                "6. 回答简洁专业，控制在500字以内");

        SYSTEM_PROMPTS.put("script",
                "你是「点金手」平台的AI话术助手，专精直播话术优化与设计。\n" +
                "你的能力包括：开场白优化、产品卖点提炼、促单催单话术、互动留人话术、福利引导话术、下播预告、话术节奏规划、痛点引导话术。\n" +
                "回复要求：\n" +
                "1. 使用Markdown格式，结构清晰\n" +
                "2. 提供2-3个不同风格的话术版本（如：福利型、悬念型、痛点型）\n" +
                "3. 话术要口语化、有感染力，适合直播场景\n" +
                "4. 标注每段话术的目的和使用时机\n" +
                "5. 给出话术使用技巧和注意事项\n" +
                "6. 回答简洁专业，控制在500字以内");

        SYSTEM_PROMPTS.put("comparison",
                "你是「点金手」平台的AI对比分析助手，专精直播间对比复盘。\n" +
                "你的能力包括：对比两场直播的营销方式、运营数据、话术质量、节奏风格、互动粉团、留人策略等。\n" +
                "回复要求：\n" +
                "1. 使用Markdown格式，结构清晰\n" +
                "2. 用表格进行维度对比\n" +
                "3. 明确指出各自的优势和不足\n" +
                "4. 给出可执行的改进建议\n" +
                "5. 回答简洁专业，控制在500字以内");
    }

    static {
        List<PresetQuestionVO> operationPresets = Arrays.asList(
                new PresetQuestionVO(1, "拆解直播框架和风格", "分析直播的整体框架设计和风格特点", "#2B6BFF"),
                new PresetQuestionVO(2, "分析目标用户画像", "分析直播间的目标用户群体特征", "#F5364B"),
                new PresetQuestionVO(3, "分析直播留人情况", "分析观众停留时长和留人话术效果", "#FF8C00"),
                new PresetQuestionVO(4, "拆解连麦情况", "分析连麦互动的效果和优化点", "#10B981"),
                new PresetQuestionVO(5, "拆解分析互动手段", "分析互动话术、互动频率和效果", "#8B5CF6"),
                new PresetQuestionVO(6, "拆解优化粉团动作", "分析粉丝团引导和优化建议", "#EC4899"),
                new PresetQuestionVO(7, "优化营销塑品方式", "分析产品塑造和营销话术效果", "#F59E0B"),
                new PresetQuestionVO(8, "分析私域导流方式", "分析私域引导话术和转化效果", "#06B6D4"),
                new PresetQuestionVO(9, "分析新粉互动和停留", "分析新关注粉丝的互动和停留情况", "#84CC16"),
                new PresetQuestionVO(10, "分析拆解人设塑造", "分析主播人设定位和塑造效果", "#F43F5E"),
                new PresetQuestionVO(11, "提炼钩子类型和话术", "提取直播中使用的钩子类型和效果", "#3B82F6"),
                new PresetQuestionVO(12, "A3人群积累优化", "分析A3人群积累策略和优化建议", "#A78BFA")
        );
        PRESETS.put("operation", operationPresets);

        List<PresetQuestionVO> compliancePresets = Arrays.asList(
                new PresetQuestionVO(1, "检测直播敏感词使用情况", "检查直播中出现的敏感词和频率", "#F53F3F"),
                new PresetQuestionVO(2, "分析是否存在虚假宣传", "检测夸大宣传和无依据的功效承诺", "#FF7D00"),
                new PresetQuestionVO(3, "检查绝对化用语风险", "排查最、第一、全网最低等绝对化表述", "#F5A623"),
                new PresetQuestionVO(4, "分析引导场外交易风险", "检测加微信、私聊等场外引导行为", "#722ED1"),
                new PresetQuestionVO(5, "检测涉政涉黄敏感内容", "排查政治、宗教、低俗等敏感话题", "#F76560"),
                new PresetQuestionVO(6, "检查商品描述合规性", "检查产品功效承诺和价格标示合规", "#0FC6C2"),
                new PresetQuestionVO(7, "分析评论区违规风险", "检测水军刷评、诱导好评等违规", "#3491FA"),
                new PresetQuestionVO(8, "生成合规修改建议", "综合生成话术和行为合规优化方案", "#00B42A")
        );
        PRESETS.put("compliance", compliancePresets);

        List<PresetQuestionVO> scriptPresets = Arrays.asList(
                new PresetQuestionVO(1, "优化开场白话术", "优化直播开场话术提升留人率", "#FF7D00"),
                new PresetQuestionVO(2, "提炼产品卖点话术", "提炼产品核心卖点和介绍话术", "#F5364B"),
                new PresetQuestionVO(3, "优化促单催单话术", "优化逼单话术提升下单转化率", "#F5A623"),
                new PresetQuestionVO(4, "生成互动留人话术", "生成粉丝互动和留人钩子话术", "#722ED1"),
                new PresetQuestionVO(5, "优化福利引导话术", "优化福袋、抽奖、赠品引导话术", "#00B42A"),
                new PresetQuestionVO(6, "生成下播预告话术", "生成收尾和预告下场直播话术", "#3491FA"),
                new PresetQuestionVO(7, "拆解话术节奏和结构", "分析话术整体结构和节奏安排", "#0FC6C2"),
                new PresetQuestionVO(8, "生成痛点引导话术", "生成痛点切入和场景化引导话术", "#EB0AA4")
        );
        PRESETS.put("script", scriptPresets);

        List<PresetQuestionVO> comparisonPresets = Arrays.asList(
                new PresetQuestionVO(1, "对比营销塑品", "对比两场直播的营销塑品方式差异", "#2B6BFF"),
                new PresetQuestionVO(2, "对比内容数据", "对比两场直播的核心运营数据", "#F5364B"),
                new PresetQuestionVO(3, "对比话术质量", "对比两场直播话术的还原度和质量", "#00B42A"),
                new PresetQuestionVO(4, "对比节奏风格", "对比两场直播的节奏和风格差异", "#722ED1"),
                new PresetQuestionVO(5, "对比互动粉团", "对比两场直播的互动和粉团运营", "#0FC6C2"),
                new PresetQuestionVO(6, "对比留人人设", "对比两场直播的留人策略和人设塑造", "#FF7D00"),
                new PresetQuestionVO(7, "对比迎新方式", "对比两场直播的迎新方式差异", "#EB0AA4"),
                new PresetQuestionVO(8, "对比痛点话术", "对比两场直播痛点话术的优劣", "#3491FA")
        );
        PRESETS.put("comparison", comparisonPresets);
    }

    private final AiConversationMapper conversationMapper;
    private final AiConfig.AiProperties aiProperties;  // 保留作为 model 回退默认值
    private final ComparisonMapper comparisonMapper;
    private final AnalysisTaskMapper analysisTaskMapper;
    private final ThirdPartySettings thirdParty;
    private final DailyAiQuotaService dailyQuota;

    public AiChatService(AiConversationMapper conversationMapper, AiConfig.AiProperties aiProperties,
                         ComparisonMapper comparisonMapper, AnalysisTaskMapper analysisTaskMapper,
                         ThirdPartySettings thirdParty, DailyAiQuotaService dailyQuota) {
        this.conversationMapper = conversationMapper;
        this.aiProperties = aiProperties;
        this.comparisonMapper = comparisonMapper;
        this.analysisTaskMapper = analysisTaskMapper;
        this.thirdParty = thirdParty;
        this.dailyQuota = dailyQuota;
    }

    @Transactional
    public ChatMessageVO chat(ChatRequest req) {
        Long userId = SecurityContextHelper.currentUserId();
        // v1.1.2：AI 助手提问也计入每日 10 次 AI 额度，超限直接拦截
        dailyQuota.checkBeforeAnalyze(userId);

        // Save user message
        AiConversation userMsg = new AiConversation();
        userMsg.setUserId(userId);
        userMsg.setTaskId(req.getTaskId());
        userMsg.setComparisonId(req.getComparisonId());
        userMsg.setAssistantType(req.getAssistantType());
        userMsg.setAiModel(req.getAiModel() != null ? req.getAiModel() : "deepseek_r1");
        userMsg.setRole("user");
        userMsg.setContent(req.getMessage());
        userMsg.setPresetQuestionId(req.getPresetQuestionId());
        userMsg.setTokensUsed(0);
        conversationMapper.insert(userMsg);

        // Call real Yunwu API
        String aiModelId = userMsg.getAiModel();
        String apiModel = MODEL_MAP.getOrDefault(aiModelId, "deepseek-chat");
        String systemPrompt = SYSTEM_PROMPTS.getOrDefault(req.getAssistantType(),
                SYSTEM_PROMPTS.get("operation"));

        // Inject live stream transcript data into system prompt
        String liveContext = buildLiveContext(req.getTaskId(), req.getComparisonId());
        if (liveContext != null && !liveContext.isEmpty()) {
            systemPrompt = systemPrompt + "\n\n" + liveContext;
        }

        // Load recent conversation history for context
        List<AiConversation> recentHistory = loadRecentHistory(
                userId, req.getTaskId(), req.getComparisonId(), req.getAssistantType());

        String aiContent;
        String thinking = null;
        int tokensUsed = 0;

        try {
            JSONObject apiResult = callYunwuApi(userId, apiModel, systemPrompt, recentHistory, req.getMessage());
            aiContent = apiResult.getStr("content", "抱歉，AI暂时无法回复，请稍后重试。");
            thinking = apiResult.getStr("thinking");
            tokensUsed = apiResult.getInt("tokensUsed", 0);
            log.info("Yunwu API success: model={}, tokens={}", apiModel, tokensUsed);
        } catch (Exception e) {
            log.error("Yunwu API call failed: model={}, error={}", apiModel, e.getMessage());
            // Fallback to mock response
            aiContent = generateMockResponse(req.getAssistantType(), req.getMessage());
            thinking = null;
            tokensUsed = aiContent.length();
        }

        AiConversation assistantMsg = new AiConversation();
        assistantMsg.setUserId(userId);
        assistantMsg.setTaskId(req.getTaskId());
        assistantMsg.setComparisonId(req.getComparisonId());
        assistantMsg.setAssistantType(req.getAssistantType());
        assistantMsg.setAiModel(userMsg.getAiModel());
        assistantMsg.setRole("assistant");
        assistantMsg.setContent(aiContent);
        assistantMsg.setThinking(thinking);
        assistantMsg.setTokensUsed(tokensUsed);
        conversationMapper.insert(assistantMsg);

        // Truncate history if needed
        truncateHistory(userId, req.getTaskId(), req.getAssistantType());

        return ChatMessageVO.fromEntity(assistantMsg);
    }

    /**
     * 调用云雾API (OpenAI兼容接口)。
     * endpoint / apiKey 从调用者 userId 对应的 user_third_party_settings 读取；
     * 未配置时 ThirdPartySettings 会抛 THIRD_PARTY_NOT_CONFIGURED 业务异常。
     */
    private JSONObject callYunwuApi(Long userId, String model, String systemPrompt,
                                     List<AiConversation> history, String userMessage) {
        String endpoint = thirdParty.getYunwuEndpoint(userId);
        String apiKey = thirdParty.getYunwuApiKey(userId);
        String url = endpoint.endsWith("/")
                ? endpoint + "chat/completions"
                : endpoint + "/chat/completions";

        // Build messages array
        JSONArray messages = new JSONArray();
        messages.add(new JSONObject().set("role", "system").set("content", systemPrompt));

        // Add conversation history for context
        for (AiConversation msg : history) {
            messages.add(new JSONObject().set("role", msg.getRole()).set("content", msg.getContent()));
        }

        // Add current user message
        messages.add(new JSONObject().set("role", "user").set("content", userMessage));

        JSONObject body = new JSONObject();
        body.set("model", model);
        body.set("messages", messages);
        body.set("temperature", 0.7);
        body.set("max_tokens", 2048);

        log.info("Calling Yunwu API: url={}, model={}, messagesCount={}", url, model, messages.size());

        HttpResponse response = HttpRequest.post(url)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .body(body.toString())
                .timeout(API_TIMEOUT_MS)
                .execute();

        if (!response.isOk()) {
            throw new RuntimeException("Yunwu API HTTP " + response.getStatus() + ": " + response.body());
        }

        JSONObject json = JSONUtil.parseObj(response.body());
        JSONArray choices = json.getJSONArray("choices");
        if (choices == null || choices.isEmpty()) {
            throw new RuntimeException("Yunwu API returned empty choices");
        }

        JSONObject firstChoice = choices.getJSONObject(0);
        JSONObject message = firstChoice.getJSONObject("message");
        String content = message.getStr("content", "");

        // Extract thinking/reasoning content if present (DeepSeek R1 etc.)
        String thinking = message.getStr("reasoning_content");

        // Get token usage
        int totalTokens = 0;
        JSONObject usage = json.getJSONObject("usage");
        if (usage != null) {
            totalTokens = usage.getInt("total_tokens", 0);
        }

        JSONObject result = new JSONObject();
        result.set("content", content);
        result.set("thinking", thinking);
        result.set("tokensUsed", totalTokens);
        return result;
    }

    /**
     * 加载最近对话历史作为上下文
     */
    private List<AiConversation> loadRecentHistory(Long userId, Long taskId,
                                                    Long comparisonId, String assistantType) {
        LambdaQueryWrapper<AiConversation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiConversation::getUserId, userId)
                .eq(AiConversation::getAssistantType, assistantType)
                .eq(AiConversation::getDeleted, 0)
                .orderByDesc(AiConversation::getId)
                .last("LIMIT " + (CONTEXT_ROUNDS * 2));

        if (taskId != null) {
            wrapper.eq(AiConversation::getTaskId, taskId);
        }
        if (comparisonId != null) {
            wrapper.eq(AiConversation::getComparisonId, comparisonId);
        }

        List<AiConversation> recent = conversationMapper.selectList(wrapper);
        // Reverse to chronological order
        Collections.reverse(recent);
        return recent;
    }

    public List<PresetQuestionVO> getPresets(String type) {
        List<PresetQuestionVO> presets = PRESETS.get(type);
        if (presets == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "无效的助手类型: " + type);
        }
        return presets;
    }

    public Map<String, String> switchModel(String aiModel) {
        if (!"doubao".equals(aiModel) && !"deepseek_r1".equals(aiModel)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "无效的AI模型: " + aiModel);
        }
        Map<String, String> result = new HashMap<>();
        result.put("aiModel", aiModel);
        result.put("message", "模型切换成功");
        return result;
    }

    public PageResult<ChatMessageVO> getHistory(Long taskId, Long comparisonId, String assistantType, int page, int size) {
        Long userId = SecurityContextHelper.currentUserId();

        LambdaQueryWrapper<AiConversation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiConversation::getUserId, userId);
        if (taskId != null) {
            wrapper.eq(AiConversation::getTaskId, taskId);
        }
        if (comparisonId != null) {
            wrapper.eq(AiConversation::getComparisonId, comparisonId);
        }
        if (assistantType != null && !assistantType.trim().isEmpty()) {
            wrapper.eq(AiConversation::getAssistantType, assistantType.trim());
        }
        wrapper.orderByAsc(AiConversation::getCreatedAt);

        Page<AiConversation> pageParam = new Page<>(page, size);
        Page<AiConversation> result = conversationMapper.selectPage(pageParam, wrapper);

        List<ChatMessageVO> items = new ArrayList<>();
        for (AiConversation conv : result.getRecords()) {
            items.add(ChatMessageVO.fromEntity(conv));
        }

        return PageResult.of(items, result.getTotal(), page, size);
    }

    private String buildLiveContext(Long taskId, Long comparisonId) {
        StringBuilder sb = new StringBuilder();

        if (comparisonId != null) {
            Comparison comparison = comparisonMapper.selectById(comparisonId);
            if (comparison != null) {
                Long taskIdOpt = comparison.getTaskIdOptimize();
                Long taskIdRef = comparison.getTaskIdReference();

                if (taskIdOpt != null) {
                    AnalysisTask taskOpt = analysisTaskMapper.selectById(taskIdOpt);
                    if (taskOpt != null && taskOpt.getAsrText() != null && !taskOpt.getAsrText().isEmpty()) {
                        sb.append("【直播A（优化场）话术记录】\n");
                        sb.append(truncateText(taskOpt.getAsrText(), 8000));
                        if (taskOpt.getSummary() != null && !taskOpt.getSummary().isEmpty()) {
                            sb.append("\n\n【直播A AI分析摘要】\n").append(taskOpt.getSummary());
                        }
                        if (taskOpt.getAiDiagnosis() != null && !taskOpt.getAiDiagnosis().isEmpty()) {
                            sb.append("\n\n【直播A AI诊断】\n").append(taskOpt.getAiDiagnosis());
                        }
                    }
                }

                if (taskIdRef != null) {
                    AnalysisTask taskRef = analysisTaskMapper.selectById(taskIdRef);
                    if (taskRef != null && taskRef.getAsrText() != null && !taskRef.getAsrText().isEmpty()) {
                        if (sb.length() > 0) {
                            sb.append("\n\n");
                        }
                        sb.append("【直播B（参考场）话术记录】\n");
                        sb.append(truncateText(taskRef.getAsrText(), 8000));
                        if (taskRef.getSummary() != null && !taskRef.getSummary().isEmpty()) {
                            sb.append("\n\n【直播B AI分析摘要】\n").append(taskRef.getSummary());
                        }
                        if (taskRef.getAiDiagnosis() != null && !taskRef.getAiDiagnosis().isEmpty()) {
                            sb.append("\n\n【直播B AI诊断】\n").append(taskRef.getAiDiagnosis());
                        }
                    }
                }

                if (comparison.getAiComparisonResult() != null && !comparison.getAiComparisonResult().isEmpty()) {
                    if (sb.length() > 0) {
                        sb.append("\n\n");
                    }
                    sb.append("【已有AI对比分析结果】\n").append(comparison.getAiComparisonResult());
                }
            }
        } else if (taskId != null) {
            AnalysisTask task = analysisTaskMapper.selectById(taskId);
            if (task != null && task.getAsrText() != null && !task.getAsrText().isEmpty()) {
                sb.append("【直播话术记录】\n");
                sb.append(truncateText(task.getAsrText(), 15000));
                if (task.getSummary() != null && !task.getSummary().isEmpty()) {
                    sb.append("\n\n【AI分析摘要】\n").append(task.getSummary());
                }
                if (task.getAiDiagnosis() != null && !task.getAiDiagnosis().isEmpty()) {
                    sb.append("\n\n【AI诊断】\n").append(task.getAiDiagnosis());
                }
            }
        }

        return sb.toString();
    }

    private String truncateText(String text, int maxChars) {
        if (text == null || text.length() <= maxChars) {
            return text;
        }
        return text.substring(0, maxChars) + "\n...(内容过长已截断)";
    }

    private void truncateHistory(Long userId, Long taskId, String assistantType) {
        LambdaQueryWrapper<AiConversation> countWrapper = new LambdaQueryWrapper<>();
        countWrapper.eq(AiConversation::getUserId, userId);
        if (taskId != null) {
            countWrapper.eq(AiConversation::getTaskId, taskId);
        }
        countWrapper.eq(AiConversation::getAssistantType, assistantType);
        countWrapper.orderByAsc(AiConversation::getCreatedAt);

        List<AiConversation> all = conversationMapper.selectList(countWrapper);
        // Each round = 2 messages (user + assistant), so max messages = MAX_HISTORY_ROUNDS * 2
        int maxMessages = MAX_HISTORY_ROUNDS * 2;
        if (all.size() > maxMessages) {
            int toRemove = all.size() - maxMessages;
            for (int i = 0; i < toRemove; i++) {
                conversationMapper.deleteById(all.get(i).getId());
            }
        }
    }

    private String generateMockResponse(String assistantType, String message) {
        // 先处理通用对话类问题
        String generalReply = handleGeneralQuestion(message, assistantType);
        if (generalReply != null) {
            return generalReply;
        }
        if ("compliance".equals(assistantType)) {
            return generateComplianceMock(message);
        } else if ("script".equals(assistantType)) {
            return generateScriptMock(message);
        } else if ("comparison".equals(assistantType)) {
            return generateComparisonMock(message);
        } else {
            return generateOperationMock(message);
        }
    }

    private String handleGeneralQuestion(String message, String assistantType) {
        String lowerMsg = message.toLowerCase();

        // 模型身份相关
        if (lowerMsg.contains("你是谁") || lowerMsg.contains("什么模型") || lowerMsg.contains("大模型")
                || lowerMsg.contains("哪个模型") || lowerMsg.contains("什么ai") || lowerMsg.contains("自我介绍")) {
            String roleMap = "operation".equals(assistantType) ? "运营助手" :
                    "compliance".equals(assistantType) ? "违规助手" :
                    "script".equals(assistantType) ? "话术助手" : "AI助手";
            return "你好！我是**点金手" + roleMap + "**，基于大语言模型驱动。\n\n" +
                    "你可以在右上角的下拉菜单中切换不同的AI模型，目前支持：\n" +
                    "- **DeepSeek R1/V3** — 中文理解顶级，性价比最高\n" +
                    "- **通义千问 Max/Plus** — 电商场景理解出色\n" +
                    "- **豆包 Pro/Lite** — 最懂抖音直播话术\n" +
                    "- **GPT-4o-mini** — 通用能力强\n" +
                    "- **智谱 GLM-4** — 结构化分析优秀\n" +
                    "- **Claude Sonnet** — 长文本分析能力强\n" +
                    "- **Moonshot** — 超长文本处理\n\n" +
                    "不同模型各有所长，你可以根据需要自由切换。有什么我能帮你的吗？";
        }

        // 打招呼
        if (lowerMsg.matches(".*(你好|hello|hi|嗨|hey|在吗|在不在).*") && lowerMsg.length() < 10) {
            String roleDesc = "operation".equals(assistantType) ? "分析直播运营数据、诊断直播间问题、优化运营策略" :
                    "compliance".equals(assistantType) ? "检测敏感词、识别虚假宣传、排查违规风险、生成合规建议" :
                    "script".equals(assistantType) ? "优化开场白、设计卖点话术、提升促单转化、改善互动技巧" : "帮助你分析直播内容";
            return "你好！我是你的AI助手，随时准备为你服务 😊\n\n" +
                    "我能帮你：" + roleDesc + "。\n\n" +
                    "你可以直接提问，或者点击下方的快捷问题按钮开始。";
        }

        // 感谢
        if (lowerMsg.matches(".*(谢谢|感谢|thanks|thank you|辛苦了).*")) {
            return "不客气！如果还有其他问题随时问我，我会尽力帮助你 😊";
        }

        // 能力边界
        if (lowerMsg.contains("你能做什么") || lowerMsg.contains("你会什么") || lowerMsg.contains("功能")
                || lowerMsg.contains("帮我什么")) {
            String capabilities = "operation".equals(assistantType) ?
                    "1. **数据分析** — 分析直播间关键运营指标\n" +
                    "2. **问题诊断** — 发现直播间运营中的薄弱环节\n" +
                    "3. **策略建议** — 提供针对性的运营优化方案\n" +
                    "4. **竞品对比** — 分析与竞品直播间的差异\n" +
                    "5. **趋势预测** — 基于数据给出运营趋势判断" :
                "compliance".equals(assistantType) ?
                    "1. **敏感词检测** — 扫描直播话术中的违规用词\n" +
                    "2. **虚假宣传识别** — 发现夸大、误导性的产品描述\n" +
                    "3. **绝对化用语排查** — 检测「最好」「第一」等违规表述\n" +
                    "4. **场外引导检测** — 识别引导加微信、场外交易等行为\n" +
                    "5. **合规建议** — 提供合规替换话术和优化方案" :
                    "1. **开场白优化** — 设计吸引人的直播开场话术\n" +
                    "2. **卖点提炼** — 提炼产品核心卖点话术\n" +
                    "3. **促单催单** — 设计高转化的逼单话术\n" +
                    "4. **互动留人** — 优化粉丝互动和留人技巧\n" +
                    "5. **话术节奏** — 规划整场直播的话术节奏";
            return "我可以帮你做以下事情：\n\n" + capabilities + "\n\n点击下方快捷问题或直接输入你的问题即可开始！";
        }

        return null; // 非通用问题，交给各助手处理
    }

    private String generateComplianceMock(String message) {
        String lowerMsg = message.toLowerCase();
        if (lowerMsg.contains("敏感词") || lowerMsg.contains("敏感")) {
            return "## 敏感词检测报告\n\n针对「" + message + "」，检测结果如下：\n\n" +
                    "### 检测到的敏感词\n- **绝对化用语**：「最好的」「第一名」「全网最低」共出现3次\n- **虚假宣传**：「100%有效」出现1次\n- **引导场外**：未检测到\n\n" +
                    "### 风险等级：⚠️ 中风险\n\n" +
                    "### 修改建议\n1. 「最好的」→「深受好评的」「广受认可的」\n2. 「全网最低」→「限时特惠价」「直播间专属价」\n3. 「100%有效」→「众多用户反馈效果明显」\n\n" +
                    "### 平台规则提示\n抖音平台对绝对化用语处罚力度较大，建议全面排查替换。";
        } else if (lowerMsg.contains("虚假") || lowerMsg.contains("宣传") || lowerMsg.contains("夸大")) {
            return "## 虚假宣传风险分析\n\n针对「" + message + "」，分析如下：\n\n" +
                    "### 高风险表述\n1. **夸大功效**：「立竿见影」「一用就白」等承诺即时效果的表述\n2. **数据无据**：「销量第一」「好评率99%」等缺乏权威数据支撑\n3. **对比贬低**：「比XX品牌好10倍」等无依据对比\n\n" +
                    "### 合规修改方案\n- 「立竿见影」→「坚持使用，效果看得见」\n- 「销量第一」→「深受消费者喜爱」\n- 删除无依据的数据对比\n\n" +
                    "### 法规依据\n《广告法》第二十八条：广告不得含有虚假或者引人误解的内容，不得欺骗、误导消费者。";
        } else if (lowerMsg.contains("绝对化") || lowerMsg.contains("用语")) {
            return "## 绝对化用语风险排查\n\n针对「" + message + "」，排查结果：\n\n" +
                    "### 常见绝对化用语\n| 违规表述 | 风险等级 | 建议替换 |\n|---------|---------|--------|\n| 最好的 | 高 | 优质的/备受好评的 |\n| 第一/No.1 | 高 | 领先的/知名的 |\n| 全网最低 | 高 | 直播间专属价 |\n| 绝对/肯定 | 中 | 通常/一般来说 |\n| 永远/不会 | 中 | 长期/极少 |\n\n" +
                    "### 本场检测\n- 检测到绝对化用语 **4处**\n- 建议全部替换，降低平台处罚风险\n\n" +
                    "### 注意\n抖音、快手平台均已加强对绝对化用语的自动检测，违规可能导致直播间降权或封禁。";
        } else if (lowerMsg.contains("场外") || lowerMsg.contains("引导") || lowerMsg.contains("导流")) {
            return "## 引导场外交易风险分析\n\n针对「" + message + "」，分析如下：\n\n" +
                    "### 高风险行为\n1. **引导加微信**：直播中提及「加我微信」「私聊我」等属于严重违规\n2. **场外链接**：分享非平台内链接，引导至第三方交易\n3. **暗示话术**：「懂的都懂」「老粉知道怎么做」等暗示性引导\n\n" +
                    "### 本场检测\n- 未发现明确的场外引导行为\n- 建议注意粉丝团引导话术边界\n\n" +
                    "### 合规提醒\n- 所有交易应在平台内完成\n- 粉丝群引导应使用平台官方粉丝群功能\n- 违反场外交易规则可能导致账号永久封禁";
        } else if (lowerMsg.contains("涉政") || lowerMsg.contains("涉黄") || lowerMsg.contains("政治") || lowerMsg.contains("敏感内容")) {
            return "## 敏感内容检测报告\n\n针对「" + message + "」，检测结果：\n\n" +
                    "### 涉政检测\n- 未检测到明确涉政表述\n- 建议避免讨论时事政治、政策评价等话题\n\n" +
                    "### 涉黄检测\n- 未检测到涉黄内容\n- 注意穿着、肢体动作等视觉合规\n\n" +
                    "### 其他敏感检测\n- 宗教相关：未检测到\n- 暴力相关：未检测到\n\n" +
                    "### 预防建议\n1. 直播前准备好话术脚本，避免即兴发挥触碰红线\n2. 避免评论国内外时事、政策\n3. 产品介绍中避免涉及医疗效果承诺";
        } else if (lowerMsg.contains("商品") || lowerMsg.contains("描述") || lowerMsg.contains("合规")) {
            return "## 商品描述合规检查\n\n针对「" + message + "」，检查结果：\n\n" +
                    "### 需要注意的描述\n1. **功效承诺**：化妆品不可宣称医疗效果（如「祛斑」「治痘」）\n2. **成分标注**：需如实标注，不可夸大成分功效\n3. **价格标示**：原价需有真实交易记录支撑\n\n" +
                    "### 合规建议\n- 使用「帮助改善」替代「彻底解决」\n- 使用「含XX成分」替代「XX成分深层修复」\n- 标注「活动价」替代「全年最低价」\n\n" +
                    "### 行业特殊规则\n- 食品类：不可宣称疾病预防/治疗功能\n- 化妆品类：不可使用医疗术语\n- 保健品类：需明确标注「本品不能代替药物」";
        } else if (lowerMsg.contains("评论") || lowerMsg.contains("评论区")) {
            return "## 评论区违规风险分析\n\n针对「" + message + "」，分析如下：\n\n" +
                    "### 常见评论区违规\n1. **水军刷评**：大量相似好评可能触发平台检测\n2. **诱导好评**：「给好评返现」「好评截图找客服领礼品」\n3. **恶意引导**：在评论区发布引流信息\n\n" +
                    "### 主播应对建议\n- 发现违规评论及时删除或举报\n- 不主动引导刷好评\n- 设置评论关键词过滤\n\n" +
                    "### 合规互动话术\n- 「用过的家人们可以分享下真实感受」\n- 「欢迎大家理性评价，帮助更多人了解产品」";
        } else if (lowerMsg.contains("建议") || lowerMsg.contains("修改") || lowerMsg.contains("优化")) {
            return "## 合规优化建议汇总\n\n针对「" + message + "」，综合建议如下：\n\n" +
                    "### 话术合规优化\n1. 全面替换绝对化用语，使用「深受好评」「广受认可」等柔性表达\n2. 功效描述加入「因人而异」「效果因人而异」等免责表述\n3. 价格宣传使用「直播间专属价」替代「全网最低价」\n\n" +
                    "### 行为合规优化\n1. 禁止引导场外交易，所有交易在平台内完成\n2. 粉丝群引导使用平台官方功能\n3. 避免在直播中讨论竞品、时政等敏感话题\n\n" +
                    "### 视觉合规\n1. 确保直播间布景符合平台规范\n2. 主播穿着得体，避免低俗\n3. 产品展示真实，不使用过度美化滤镜\n\n" +
                    "### 合规自查清单\n- [ ] 话术脚本已排查绝对化用语\n- [ ] 产品描述已合规化处理\n- [ ] 价格标示有真实依据\n- [ ] 无场外引导行为";
        } else {
            return "## 合规分析\n\n针对您提出的「" + message + "」，综合分析如下：\n\n" +
                    "### 整体合规评估\n- **话术合规度**：⭐⭐⭐☆☆（中等）\n- **行为合规度**：⭐⭐⭐⭐☆（良好）\n- **商品描述合规度**：⭐⭐⭐☆☆（中等）\n\n" +
                    "### 主要风险点\n1. 发现3处绝对化用语，建议替换\n2. 产品功效描述有夸大嫌疑，需要调整\n3. 部分价格对比缺乏依据\n\n" +
                    "### 优化建议\n- 「最好的」→「深受好评的」\n- 「100%有效」→「众多用户反馈效果明显」\n- 「全网最低价」→「限时优惠价」\n\n" +
                    "### 后续建议\n建议在每场直播前进行话术脚本合规审查，可使用本助手逐一检测各项内容。";
        }
    }

    private String generateScriptMock(String message) {
        String lowerMsg = message.toLowerCase();
        if (lowerMsg.contains("开场") || lowerMsg.contains("开播")) {
            return "## 开场白话术优化\n\n针对「" + message + "」，优化方案如下：\n\n" +
                    "### 原始开场（待优化）\n> 大家好，欢迎来到直播间\n\n" +
                    "### 优化版本A（福利型）\n> 「家人们好！今天直播间准备了3波福利！第一波：前100名下单的宝子直接送XX！先别急着走，听完再决定！」\n\n" +
                    "### 优化版本B（悬念型）\n> 「来了来了！今天给大家带来一个我用了半年的好东西，先不说是什么，你们猜猜看？猜对了直接送！」\n\n" +
                    "### 优化版本C（痛点型）\n> 「是不是经常遇到XX问题？今天就帮你们彻底解决！认真听完，绝对值得！」\n\n" +
                    "### 开场黄金法则\n1. 前5秒抛出利益点或悬念\n2. 前30秒完成留人钩子设置\n3. 1分钟内建立信任感";
        } else if (lowerMsg.contains("卖点") || lowerMsg.contains("产品")) {
            return "## 产品卖点话术提炼\n\n针对「" + message + "」，优化方案如下：\n\n" +
                    "### 卖点提炼公式\n**痛点场景 + 产品解决方案 + 效果验证 + 限时优惠**\n\n" +
                    "### 话术模板\n> 「很多姐妹跟我说（痛点），试了很多方法都不行。今天这款（产品），它的核心成分是（卖点），我自己用了X个月（验证），现在直播间专属价只要XX元（促单）！」\n\n" +
                    "### 进阶技巧\n1. **FABE法则**：特征→优势→利益→证据\n2. **对比法**：「别的地方卖XX，今天只要XX」\n3. **场景法**：描述使用场景，让用户产生代入感\n4. **数据法**：「已经卖出XXX件，好评率XX%」";
        } else if (lowerMsg.contains("促单") || lowerMsg.contains("催单") || lowerMsg.contains("逼单")) {
            return "## 促单催单话术优化\n\n针对「" + message + "」，优化方案如下：\n\n" +
                    "### 倒计时逼单\n> 「最后30秒！错过今天这个价格，下次至少多花50块！3、2、1，改价！」\n\n" +
                    "### 限量逼单\n> 「就剩最后XX单了！抢到就是赚到，手慢无！别犹豫了家人们！」\n\n" +
                    "### 从众逼单\n> 「已经有800多人下单了！这个价格闭眼入都不会错，还在犹豫的赶紧！」\n\n" +
                    "### 损失厌恶逼单\n> 「今天不买，明天恢复原价你就得多花XX块，省下来的钱够吃好几顿饭了！」\n\n" +
                    "### 连环逼单公式\n1. 第一轮：价值塑造（这个东西值多少）\n2. 第二轮：价格锚定（原价 vs 直播价）\n3. 第三轮：限时限量（只有XX个/只到今晚）\n4. 第四轮：下单指引（教用户怎么拍）";
        } else if (lowerMsg.contains("互动") || lowerMsg.contains("留人")) {
            return "## 互动留人话术生成\n\n针对「" + message + "」，优化方案如下：\n\n" +
                    "### 新人留人话术\n> 「新来的宝子先别走！今天直播间有专属新人福利，关注+加粉丝团就能领！」\n\n" +
                    "### 互动引导话术\n> 「觉得这个价格可以的扣1，觉得还能再低的扣2，让我看看大家的想法！」\n> 「用过的家人们来说说效果怎么样，帮新来的宝子做个参考！」\n\n" +
                    "### 粉丝团引导\n> 「加了粉丝团的宝子优先发货！还没加的赶紧，点下面那个小心心就行！」\n\n" +
                    "### 留人钩子话术\n> 「等一下有个超级大福利，我先不说是什么，但是现在走的人绝对会后悔！」\n> 「下一款是今天的压轴爆款，千万别走！」";
        } else if (lowerMsg.contains("福利") || lowerMsg.contains("抽奖")) {
            return "## 福利引导话术优化\n\n针对「" + message + "」，优化方案如下：\n\n" +
                    "### 福袋/抽奖话术\n> 「家人们注意！马上开福袋了！点赞到XX万直接开！现在赶紧点点赞，离目标就差一点了！」\n\n" +
                    "### 赠品引导话术\n> 「现在下单的前50名，额外送价值XX元的赠品！赠完即止，先到先得！」\n\n" +
                    "### 粉丝专属福利\n> 「这个福利只给粉丝团的家人们！没加粉丝团的赶紧加，不然领不到！」\n\n" +
                    "### 福利节奏建议\n1. 每15-20分钟安排一波福利\n2. 福利前做预告，制造期待感\n3. 福利后做总结，引导下一波期待\n4. 大福利放在流量高峰时段";
        } else if (lowerMsg.contains("下播") || lowerMsg.contains("结尾") || lowerMsg.contains("预告")) {
            return "## 下播预告话术生成\n\n针对「" + message + "」，优化方案如下：\n\n" +
                    "### 下播预告话术\n> 「今天的直播就到这里了！感谢每一位家人的陪伴！明天晚上8点，我准备了更大的惊喜，提前关注不迷路！」\n\n" +
                    "### 预告下场福利\n> 「明天的福利力度比今天还大！记得明天准时来，我会准备XX份免单！」\n\n" +
                    "### 收尾转化话术\n> 「还没下单的宝子抓紧了！今天的价格过了12点就恢复原价了，最后的机会！」\n\n" +
                    "### 收尾注意事项\n1. 提前10分钟做下播预告\n2. 收尾时做最后一波促单\n3. 预告下场直播时间和福利\n4. 感谢互动活跃的粉丝，增强粘性";
        } else if (lowerMsg.contains("节奏") || lowerMsg.contains("结构") || lowerMsg.contains("拆解")) {
            return "## 话术节奏与结构拆解\n\n针对「" + message + "」，分析如下：\n\n" +
                    "### 标准直播话术结构\n```\n开场暖场（5min）→ 福利预告（2min）→ 产品A讲解（8min）→ 互动（3min）\n→ 产品B讲解（8min）→ 福利兑现（5min）→ 产品C讲解（8min）\n→ 互动+留人（3min）→ 收尾促单（5min）→ 预告下播（3min）\n```\n\n" +
                    "### 节奏要点\n1. **每5-8分钟切换节奏**，避免观众疲劳\n2. **福利穿插在产品讲解之间**，维持在线人数\n3. **高潮点安排在流量高峰**（通常开播30分钟左右）\n4. **互动环节不超过3分钟**，保持紧凑\n\n" +
                    "### 本场话术节奏评估\n- 切换频率：适中\n- 福利节奏：略偏少，建议增加\n- 互动频率：偏低，建议每5分钟一次";
        } else if (lowerMsg.contains("痛点")) {
            return "## 痛点引导话术生成\n\n针对「" + message + "」，优化方案如下：\n\n" +
                    "### 痛点引导公式\n**描述痛点场景 → 放大痛点 → 提出解决方案 → 展示效果**\n\n" +
                    "### 话术示例\n> 「是不是每次XX的时候都特别烦？试了很多方法都没用？你不是一个人，我之前也是这样。直到遇到了这款产品，真的改变了我！」\n\n" +
                    "### 常用痛点切入角度\n1. **时间痛点**：「每天花XX分钟在这上面，太浪费时间了」\n2. **金钱痛点**：「之前买了好多都踩雷，白花了好多钱」\n3. **效果痛点**：「用了很多都没效果，都快放弃了」\n4. **社交痛点**：「出门总是不自信，就因为XX」\n\n" +
                    "### 痛点话术注意事项\n- 描述要具体，让用户产生共鸣\n- 放大痛点但不过度焦虑营销\n- 解决方案要自然过渡，不要太生硬";
        } else {
            return "## 话术优化建议\n\n针对您提出的「" + message + "」，综合优化建议：\n\n" +
                    "### 整体话术评估\n- **开场白**：⭐⭐⭐☆☆ 可增加利益点前置\n- **产品介绍**：⭐⭐⭐⭐☆ 描述清晰，可加场景化\n- **促单话术**：⭐⭐⭐☆☆ 紧迫感不够，建议加倒计时\n- **互动话术**：⭐⭐⭐⭐☆ 频率合适，形式可更丰富\n\n" +
                    "### 核心优化建议\n1. **开场前5秒**抛出核心利益点，提升停留\n2. **产品讲解**加入真实使用场景描述\n3. **促单环节**增加限时限量+倒计时\n4. **互动环节**尝试投票、猜价格等新形式\n\n" +
                    "### 话术模板推荐\n- 开场：「家人们！今天准备了X波福利，第一波XX，先别走！」\n- 过渡：「接下来这款是今天的重头戏，准备好了吗？」\n- 促单：「最后X单了！3、2、1，上链接！」\n- 收尾：「今天的福利都给到了，明天还有更大的惊喜！」";
        }
    }

    private String generateOperationMock(String message) {
        String lowerMsg = message.toLowerCase();
        if (lowerMsg.contains("框架") || lowerMsg.contains("风格")) {
            return "## 直播框架与风格分析\n\n针对「" + message + "」，分析如下：\n\n" +
                    "### 整体框架\n本场直播采用**「开场预热→产品讲解→互动引导→限时促销→收尾回顾」**五段式框架\n\n" +
                    "### 风格特征\n- **语言风格**：亲切口语化，大量使用「家人们」「宝子们」等称呼\n- **节奏把控**：每5-8分钟切换一次话题，节奏紧凑\n- **互动频率**：平均每3分钟发起一次互动指令\n\n" +
                    "### 优化建议\n1. 开场阶段可增加利益点前置，提升留人率\n2. 建议在产品讲解环节加入更多场景化描述\n3. 收尾阶段可设置预告下场福利";
        } else if (lowerMsg.contains("用户画像") || lowerMsg.contains("用户")) {
            return "## 目标用户画像分析\n\n针对「" + message + "」，分析如下：\n\n" +
                    "### 用户特征\n- **年龄层**：主要集中在25-40岁女性\n- **消费能力**：中等偏上，客单价集中在80-200元\n- **活跃时段**：晚8-10点活跃度最高\n\n" +
                    "### 用户行为\n- 评论互动积极，关注品质和性价比\n- 复购率较高，对主播信任度高\n\n" +
                    "### 建议\n1. 增加针对核心年龄段的产品选品\n2. 在黄金时段加大促销力度";
        } else if (lowerMsg.contains("留人") || lowerMsg.contains("停留")) {
            return "## 直播留人情况分析\n\n针对「" + message + "」，分析如下：\n\n" +
                    "### 留人数据\n- 平均停留时长：预估2-3分钟\n- 开场5分钟内流失率较高\n\n" +
                    "### 留人话术\n- 使用了福利预告型留人话术：「等一下有超级福利，千万别走」\n- 悬念型留人：「接下来要上的这款，绝对让你惊喜」\n\n" +
                    "### 优化建议\n1. 开场前30秒内明确告知核心利益点\n2. 每10分钟循环一次留人钩子\n3. 增加倒计时紧迫感话术";
        } else if (lowerMsg.contains("互动")) {
            return "## 互动手段分析\n\n针对「" + message + "」，分析如下：\n\n" +
                    "### 互动形式\n1. **扣字互动**：「想要的扣1」「觉得好看的扣好看」\n2. **提问互动**：「你们平时用什么牌子的？」\n3. **福利互动**：「关注+粉丝团，抽免单」\n\n" +
                    "### 互动效果\n- 扣字互动响应率最高\n- 福利互动能有效提升粉丝团加入率\n\n" +
                    "### 优化建议\n1. 互动指令要简单明确，降低参与门槛\n2. 增加实时数据反馈「已经有500人扣1了」\n3. 设置阶梯式互动奖励机制";
        } else if (lowerMsg.contains("营销") || lowerMsg.contains("塑品")) {
            return "## 营销塑品方式分析\n\n针对「" + message + "」，分析如下：\n\n" +
                    "### 塑品手法\n- **痛点切入**：先描述用户痛点场景，再引出产品解决方案\n- **对比法**：与其他品牌/渠道价格对比，凸显性价比\n- **体验展示**：现场试用/试穿，增强真实感\n\n" +
                    "### 优化建议\n1. 增加用户证言和好评截图展示\n2. 强化限量/限时的紧迫感话术\n3. 设置价格锚点，先报原价再给优惠价";
        } else if (lowerMsg.contains("粉团") || lowerMsg.contains("粉丝团")) {
            return "## 粉团动作分析\n\n针对「" + message + "」，分析如下：\n\n" +
                    "### 粉团引导\n- 直播中多次提及加入粉丝团的专属福利\n- 使用差异化权益吸引：「粉丝团专属价」「粉丝团优先发货」\n\n" +
                    "### 优化建议\n1. 设置粉丝团等级特权，激励持续活跃\n2. 定期粉丝团专属直播，增强归属感\n3. 粉丝团灯牌点亮后立即给予正向反馈";
        } else {
            return "## 运营分析\n\n针对您提出的「" + message + "」，分析如下：\n\n" +
                    "### 关键发现\n1. 直播整体节奏把控良好，话术流畅度高\n2. 互动环节可以进一步加强，当前互动频率偏低\n3. 产品介绍话术有优化空间，可增加场景化描述\n4. 留人钩子设计合理，但频率可适当提升\n\n" +
                    "### 优化建议\n- 增加互动频率，每3-5分钟设置一次互动\n- 强化产品卖点的情感化表达\n- 适当增加限时优惠的紧迫感\n- 开场前30秒内植入核心卖点";
        }
    }

    private String generateComparisonMock(String message) {
        String lowerMsg = message.toLowerCase();
        if (lowerMsg.contains("营销") || lowerMsg.contains("塑品")) {
            return "## 对比分析：营销塑品方式\n\n" +
                    "### 优化场（A主播）\n- 以痛点切入为主，话术直接，转化效率高\n- 价格锚点设置合理，对比感强\n\n" +
                    "### 参考场（B主播）\n- 以体验展示为主，注重产品细节展示\n- 用户互动更频繁，信任感建立更好\n\n" +
                    "### 对比结论\n- A主播在转化效率上更优，B主播在用户信任度上更强\n- 建议A主播增加体验展示环节，B主播增加价格锚点话术";
        } else if (lowerMsg.contains("数据") || lowerMsg.contains("内容")) {
            return "## 对比分析：内容数据\n\n" +
                    "### 核心指标对比\n| 指标 | 优化场 | 参考场 |\n|------|--------|--------|\n| 平均在线 | 较高 | 中等 |\n| 互动率 | 中等 | 较高 |\n| 转化率 | 较高 | 中等 |\n\n" +
                    "### 分析\n- 优化场在流量获取和转化上更优\n- 参考场在用户互动和粉丝粘性上更好\n\n" +
                    "### 建议\n结合两者优势，在保持高转化的同时增加互动环节";
        } else if (lowerMsg.contains("话术")) {
            return "## 对比分析：话术质量\n\n" +
                    "### 优化场特点\n- 话术简练直接，促单效率高\n- 善用倒计时和限量话术\n- 口头禅较多，可精简\n\n" +
                    "### 参考场特点\n- 话术丰富细腻，场景感强\n- 善用故事化表达建立信任\n- 节奏偏慢，可适当加快\n\n" +
                    "### 对比建议\n1. 优化场可学习参考场的场景化描述方式\n2. 参考场可学习优化场的高效促单话术";
        } else if (lowerMsg.contains("节奏") || lowerMsg.contains("风格")) {
            return "## 对比分析：节奏风格\n\n" +
                    "### 优化场\n- 快节奏，每5分钟一个产品\n- 情绪高昂，善用激情调动气氛\n- 适合冲动型消费场景\n\n" +
                    "### 参考场\n- 慢节奏，注重深度讲解\n- 亲切温和，注重情感连接\n- 适合高客单价、高决策成本产品\n\n" +
                    "### 建议\n根据产品特性选择节奏风格，低客单快节奏，高客单慢节奏";
        } else {
            return "## 对比分析\n\n针对您提出的「" + message + "」，双场对比如下：\n\n" +
                    "### 优化场亮点\n- 转化话术高效，节奏把控好\n- 善用限时福利制造紧迫感\n\n" +
                    "### 参考场亮点\n- 互动氛围好，粉丝粘性高\n- 产品讲解细腻，场景感强\n\n" +
                    "### 互相学习建议\n1. 优化场可增加互动频率和情感化表达\n2. 参考场可增加限时促销和转化引导话术\n3. 两场均可加强粉丝团专属权益设计";
        }
    }
}
