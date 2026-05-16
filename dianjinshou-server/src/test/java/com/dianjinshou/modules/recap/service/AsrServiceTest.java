package com.dianjinshou.modules.recap.service;

import com.dianjinshou.common.enums.AnalysisStatus;
import com.dianjinshou.common.enums.RecapType;
import com.dianjinshou.integration.asr.AsrClient;
import com.dianjinshou.integration.asr.AsrClient.AsrSegmentResult;
import com.dianjinshou.modules.recap.entity.AnalysisTask;
import com.dianjinshou.modules.recap.entity.AsrParagraph;
import com.dianjinshou.modules.recap.mapper.AnalysisTaskMapper;
import com.dianjinshou.modules.recap.mapper.AsrParagraphMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AsrServiceTest {

    @Mock
    private AsrClient asrClient;
    @Mock
    private AsrParagraphMapper asrParagraphMapper;
    @Mock
    private AnalysisTaskMapper analysisTaskMapper;

    @InjectMocks
    private AsrService asrService;

    @Test
    void processAsr_success() {
        AnalysisTask task = buildTask(1L);

        List<AsrSegmentResult> segments = Arrays.asList(
                new AsrSegmentResult(0, "00:00:00", "00:00:59", "第一段话术内容"),
                new AsrSegmentResult(1, "00:01:00", "00:01:59", "第二段话术内容")
        );
        when(asrClient.transcribe("/path/to/audio.wav")).thenReturn(segments);
        when(asrParagraphMapper.insert(any(AsrParagraph.class))).thenReturn(1);
        when(analysisTaskMapper.updateById(any(AnalysisTask.class))).thenReturn(1);

        List<AsrParagraph> result = asrService.processAsr(task, "/path/to/audio.wav");

        assertEquals(2, result.size());
        assertEquals(0, result.get(0).getParagraphIndex());
        assertEquals("第一段话术内容", result.get(0).getTextContent());
        assertEquals(7, result.get(0).getWordCount());

        verify(asrParagraphMapper, times(2)).insert(any(AsrParagraph.class));
        verify(analysisTaskMapper).updateById(any(AnalysisTask.class));
    }

    @Test
    void processAsr_emptyResult() {
        AnalysisTask task = buildTask(1L);

        when(asrClient.transcribe("/path/to/audio.wav")).thenReturn(Collections.emptyList());
        when(analysisTaskMapper.updateById(any(AnalysisTask.class))).thenReturn(1);

        List<AsrParagraph> result = asrService.processAsr(task, "/path/to/audio.wav");

        assertTrue(result.isEmpty());
        assertEquals(0, task.getAsrWordCount());
        verify(asrParagraphMapper, never()).insert(any(AsrParagraph.class));
    }

    @Test
    void processAsr_retriesOnFailure() {
        AnalysisTask task = buildTask(1L);

        when(asrClient.transcribe("/path/to/audio.wav"))
                .thenThrow(new RuntimeException("网络错误"))
                .thenReturn(Arrays.asList(
                        new AsrSegmentResult(0, "00:00:00", "00:00:59", "重试后成功")
                ));
        when(asrParagraphMapper.insert(any(AsrParagraph.class))).thenReturn(1);
        when(analysisTaskMapper.updateById(any(AnalysisTask.class))).thenReturn(1);

        List<AsrParagraph> result = asrService.processAsr(task, "/path/to/audio.wav");

        assertEquals(1, result.size());
        verify(asrClient, times(2)).transcribe("/path/to/audio.wav");
    }

    @Test
    void processAsr_failsAfterMaxRetries() {
        AnalysisTask task = buildTask(1L);

        when(asrClient.transcribe("/path/to/audio.wav"))
                .thenThrow(new RuntimeException("持续失败"));

        assertThrows(RuntimeException.class,
                () -> asrService.processAsr(task, "/path/to/audio.wav"));

        // 1 initial + 2 retries = 3 total attempts
        verify(asrClient, times(3)).transcribe("/path/to/audio.wav");
    }

    @Test
    void processAsr_updatesTaskAsrText() {
        AnalysisTask task = buildTask(1L);

        List<AsrSegmentResult> segments = Arrays.asList(
                new AsrSegmentResult(0, "00:00:00", "00:00:59", "段落一"),
                new AsrSegmentResult(1, "00:01:00", "00:01:59", "段落二")
        );
        when(asrClient.transcribe(any())).thenReturn(segments);
        when(asrParagraphMapper.insert(any(AsrParagraph.class))).thenReturn(1);
        when(analysisTaskMapper.updateById(any(AnalysisTask.class))).thenReturn(1);

        asrService.processAsr(task, "/path/to/audio.wav");

        assertEquals("段落一\n段落二", task.getAsrText());
        assertEquals(6, task.getAsrWordCount()); // 3 + 3
    }

    private AnalysisTask buildTask(Long id) {
        AnalysisTask task = new AnalysisTask();
        task.setId(id);
        task.setRecordingId(10L);
        task.setUserId(1L);
        task.setOrgId(5L);
        task.setType(RecapType.FULL.getCode());
        task.setStatus(AnalysisStatus.ASR_PROCESSING.getCode());
        task.setAsrWordCount(0);
        task.setCreatedAt(LocalDateTime.of(2025, 1, 1, 10, 0));
        return task;
    }
}
