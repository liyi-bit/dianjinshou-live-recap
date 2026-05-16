package com.dianjinshou.modules.recap.service;

import com.dianjinshou.common.enums.AnalysisStatus;
import com.dianjinshou.common.enums.RecapType;
import com.dianjinshou.integration.ai.AiAnalysisClient;
import com.dianjinshou.integration.ai.AiAnalysisClient.AiAnalysisResult;
import com.dianjinshou.modules.recap.entity.AnalysisTask;
import com.dianjinshou.modules.recap.entity.Keyword;
import com.dianjinshou.modules.recap.mapper.AnalysisTaskMapper;
import com.dianjinshou.modules.recap.mapper.KeywordMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiAnalysisServiceTest {

    @Mock
    private AiAnalysisClient aiClient;
    @Mock
    private AnalysisTaskMapper analysisTaskMapper;
    @Mock
    private KeywordMapper keywordMapper;

    @InjectMocks
    private AiAnalysisService aiAnalysisService;

    @Test
    void processAiAnalysis_success() {
        AnalysisTask task = buildTask(1L);
        task.setAsrText("这是一段测试文本，用于AI分析");
        task.setIndustry("知识付费");

        AiAnalysisResult mockResult = new AiAnalysisResult();
        mockResult.setAiResult("{\"dimensions\":{}}");
        mockResult.setAiDiagnosis("{\"strengths\":[]}");
        mockResult.setKeywordSummary("{\"operational\":{\"互动力\":[\"点赞\",\"关注\"]},\"sensitive\":{\"绝对化\":[\"最\"]}}");
        mockResult.setContentCompass("{\"speechRate\":\"中等\"}");
        mockResult.setSummary("测试总结");
        mockResult.setConsumedChars(100);

        when(aiClient.analyze(any(String.class), nullable(String.class), any(String.class))).thenReturn(mockResult);
        when(analysisTaskMapper.updateById(any(AnalysisTask.class))).thenReturn(1);
        when(keywordMapper.insert(any(Keyword.class))).thenReturn(1);

        aiAnalysisService.processAiAnalysis(task);

        assertEquals("{\"dimensions\":{}}", task.getAiResult());
        assertEquals("{\"strengths\":[]}", task.getAiDiagnosis());
        assertEquals("测试总结", task.getSummary());
        assertEquals(100L, task.getConsumedChars());
        // 2 updates: one for AI results, one for sensitive count
        verify(analysisTaskMapper, times(2)).updateById(any(AnalysisTask.class));
        // 3 keywords inserted (2 operational + 1 sensitive)
        verify(keywordMapper, times(3)).insert(any(Keyword.class));
    }

    @Test
    void processAiAnalysis_emptyAsrText() {
        AnalysisTask task = buildTask(1L);
        task.setAsrText("");

        aiAnalysisService.processAiAnalysis(task);

        verify(aiClient, never()).analyze(anyString(), anyString(), anyString());
        verify(analysisTaskMapper, never()).updateById(any());
    }

    @Test
    void processAiAnalysis_nullAsrText() {
        AnalysisTask task = buildTask(1L);
        task.setAsrText(null);

        aiAnalysisService.processAiAnalysis(task);

        verify(aiClient, never()).analyze(anyString(), anyString(), anyString());
    }

    @Test
    void processAiAnalysis_accumulatesConsumedChars() {
        AnalysisTask task = buildTask(1L);
        task.setAsrText("测试文本");
        task.setConsumedChars(50L);

        AiAnalysisResult mockResult = new AiAnalysisResult();
        mockResult.setAiResult("{}");
        mockResult.setAiDiagnosis("{}");
        mockResult.setKeywordSummary("{}");
        mockResult.setContentCompass("{}");
        mockResult.setSummary("总结");
        mockResult.setConsumedChars(200);

        when(aiClient.analyze(any(String.class), nullable(String.class), any(String.class))).thenReturn(mockResult);
        when(analysisTaskMapper.updateById(any(AnalysisTask.class))).thenReturn(1);

        aiAnalysisService.processAiAnalysis(task);

        assertEquals(250L, task.getConsumedChars()); // 50 + 200
    }

    private AnalysisTask buildTask(Long id) {
        AnalysisTask task = new AnalysisTask();
        task.setId(id);
        task.setRecordingId(10L);
        task.setUserId(1L);
        task.setOrgId(5L);
        task.setType(RecapType.FULL.getCode());
        task.setStatus(AnalysisStatus.AI_PROCESSING.getCode());
        task.setConsumedChars(0L);
        task.setCreatedAt(LocalDateTime.of(2025, 1, 1, 10, 0));
        return task;
    }
}
