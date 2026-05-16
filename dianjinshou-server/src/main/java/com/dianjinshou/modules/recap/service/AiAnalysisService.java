package com.dianjinshou.modules.recap.service;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dianjinshou.integration.ai.AiAnalysisClient;
import com.dianjinshou.integration.ai.AiAnalysisClient.AiAnalysisResult;
import com.dianjinshou.modules.admin.service.DailyAiQuotaService;
import com.dianjinshou.modules.recap.entity.AnalysisTask;
import com.dianjinshou.modules.recap.entity.AsrParagraph;
import com.dianjinshou.modules.recap.entity.Keyword;
import com.dianjinshou.modules.recap.mapper.AnalysisTaskMapper;
import com.dianjinshou.modules.recap.mapper.AsrParagraphMapper;
import com.dianjinshou.modules.recap.mapper.KeywordMapper;
import com.dianjinshou.modules.recording.entity.Recording;
import com.dianjinshou.modules.recording.mapper.RecordingMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class AiAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(AiAnalysisService.class);

    private static final String ANALYSIS_PROMPT_TEMPLATE =
            "你是一个专业的直播复盘分析师。请对以下直播话术转写文本进行12维度分析。\n" +
            "行业：%s\n\n" +
            "转写文本：\n%s\n\n" +
            "请从以下12个维度进行评分（0-100分）并给出诊断建议：\n" +
            "1.开场力 2.留人力 3.塑品力 4.促单力 5.互动力 6.表达力\n" +
            "7.合规性 8.粉丝团 9.私域引导 10.人设塑造 11.收尾力 12.综合评分\n\n" +
            "同时提取运营关键词和敏感词，输出JSON格式结果。";

    private final AiAnalysisClient aiClient;
    private final AnalysisTaskMapper analysisTaskMapper;
    private final AsrParagraphMapper asrParagraphMapper;
    private final KeywordMapper keywordMapper;
    private final RecordingMapper recordingMapper;
    private final DailyAiQuotaService dailyQuota;

    public AiAnalysisService(AiAnalysisClient aiClient,
                             AnalysisTaskMapper analysisTaskMapper,
                             AsrParagraphMapper asrParagraphMapper,
                             KeywordMapper keywordMapper,
                             RecordingMapper recordingMapper,
                             DailyAiQuotaService dailyQuota) {
        this.aiClient = aiClient;
        this.analysisTaskMapper = analysisTaskMapper;
        this.asrParagraphMapper = asrParagraphMapper;
        this.keywordMapper = keywordMapper;
        this.recordingMapper = recordingMapper;
        this.dailyQuota = dailyQuota;
    }

    @Transactional
    public void processAiAnalysis(AnalysisTask task) {
        String asrText = task.getAsrText();
        if (asrText == null || asrText.trim().isEmpty()) {
            log.warn("No ASR text available for task {}, skipping AI analysis", task.getId());
            return;
        }

        // v1.1.0：每日配额 check 下沉到 AnalysisService.startAiAnalysis 入口统一做，
        // 这里仅做二次兜底（老 /submit-asr autoAnalyze=true 或 re-analyze 路径没走 startAiAnalysis）
        dailyQuota.checkBeforeAnalyze(task.getUserId());

        String prompt = String.format(ANALYSIS_PROMPT_TEMPLATE,
                task.getIndustry() != null ? task.getIndustry() : "通用",
                asrText);

        AiAnalysisResult result = aiClient.analyze(task.getUserId(), asrText, task.getIndustry(), prompt);

        // Save results to task
        task.setAiResult(result.getAiResult());
        task.setAiDiagnosis(result.getAiDiagnosis());
        task.setKeywordSummary(result.getKeywordSummary());
        task.setContentCompass(result.getContentCompass());
        task.setSummary(result.getSummary());
        task.setConsumedChars(
                (task.getConsumedChars() != null ? task.getConsumedChars() : 0)
                + result.getConsumedChars());

        analysisTaskMapper.updateById(task);

        // Extract and save keywords from AI result
        saveKeywordsFromResult(task);

        // Classify paragraphs into script categories
        classifyParagraphCategories(task.getUserId(), task.getId());

        // Generate optimized text
        generateOptimizedText(task);

        // AI 分析成功完成 → 每日配额 +1（豁免用户不计）
        dailyQuota.consumeAfterAnalyze(task.getUserId());

        log.info("AI analysis completed for taskId={}, consumedChars={}",
                task.getId(), result.getConsumedChars());
    }

    private void classifyParagraphCategories(Long userId, Long taskId) {
        List<AsrParagraph> paragraphs = asrParagraphMapper.selectList(
                new LambdaQueryWrapper<AsrParagraph>()
                        .eq(AsrParagraph::getTaskId, taskId)
                        .orderByAsc(AsrParagraph::getParagraphIndex));

        if (paragraphs.isEmpty()) {
            return;
        }

        List<String> texts = new ArrayList<>();
        for (AsrParagraph p : paragraphs) {
            texts.add(p.getTextContent());
        }

        try {
            List<String> categories = aiClient.classifyParagraphs(userId, texts);
            if (categories != null && categories.size() == paragraphs.size()) {
                for (int i = 0; i < paragraphs.size(); i++) {
                    AsrParagraph p = paragraphs.get(i);
                    p.setScriptCategory(categories.get(i));
                    asrParagraphMapper.updateById(p);
                }
                log.info("Classified {} paragraphs for taskId={}", paragraphs.size(), taskId);
            } else {
                log.warn("Paragraph classification returned null or size mismatch for taskId={}", taskId);
            }
        } catch (Exception e) {
            log.warn("Failed to classify paragraphs for taskId={}: {}", taskId, e.getMessage());
        }
    }

    private void generateOptimizedText(AnalysisTask task) {
        String asrText = task.getAsrText();
        if (asrText == null || asrText.trim().isEmpty()) {
            return;
        }
        try {
            String optimized = aiClient.optimizeText(task.getUserId(), asrText);
            if (optimized != null && !optimized.trim().isEmpty()) {
                task.setOptimizedText(optimized);
                analysisTaskMapper.updateById(task);
                log.info("Optimized text generated for taskId={}, length={}", task.getId(), optimized.length());
            }
        } catch (Exception e) {
            log.warn("Failed to generate optimized text for taskId={}: {}", task.getId(), e.getMessage());
        }
    }

    private void saveKeywordsFromResult(AnalysisTask task) {
        String keywordJson = task.getKeywordSummary();
        if (keywordJson == null || keywordJson.trim().isEmpty()) {
            return;
        }

        int sensitiveCount = 0;
        int operationalCount = 0;
        try {
            JSONObject keywords = JSONUtil.parseObj(keywordJson);

            // Save operational keywords
            JSONObject operational = keywords.getJSONObject("operational");
            if (operational != null) {
                Set<String> categories = operational.keySet();
                for (String category : categories) {
                    for (Object word : operational.getJSONArray(category)) {
                        saveKeyword(task.getId(), "operational", category, null,
                                word.toString(), 1);
                        operationalCount++;
                    }
                }
            }

            // Save sensitive keywords
            JSONObject sensitive = keywords.getJSONObject("sensitive");
            if (sensitive != null) {
                Set<String> categories = sensitive.keySet();
                for (String category : categories) {
                    for (Object word : sensitive.getJSONArray(category)) {
                        saveKeyword(task.getId(), "sensitive", category, null,
                                word.toString(), 1);
                        sensitiveCount++;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to parse keywords from AI result for taskId={}: {}",
                    task.getId(), e.getMessage());
        }

        task.setSensitiveCount(sensitiveCount);
        analysisTaskMapper.updateById(task);

        // 回填到 recording 表，前端 AI 助手列表才能展示这两个数。
        // 仅 full 类型回填，避免切片任务覆盖整场计数。
        if ("full".equalsIgnoreCase(task.getType()) && task.getRecordingId() != null) {
            Recording r = recordingMapper.selectById(task.getRecordingId());
            if (r != null) {
                r.setSensitiveWordCount(sensitiveCount);
                r.setOperationKeywordCount(operationalCount);
                recordingMapper.updateById(r);
            }
        }
    }

    private void saveKeyword(Long taskId, String type, String category,
                             String subCategory, String word, int count) {
        Keyword keyword = new Keyword();
        keyword.setTaskId(taskId);
        keyword.setType(type);
        keyword.setCategory(category);
        keyword.setSubCategory(subCategory);
        keyword.setWord(word);
        keyword.setHitCountVideo1(count);
        keyword.setTotalCount(count);
        keyword.setSource("system");
        keywordMapper.insert(keyword);
    }
}
