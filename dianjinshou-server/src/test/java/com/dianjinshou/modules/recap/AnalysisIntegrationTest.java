package com.dianjinshou.modules.recap;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.dianjinshou.common.exception.BusinessException;
import com.dianjinshou.common.security.SecurityUser;
import com.dianjinshou.modules.recap.dto.CreateClipAnalysisRequest;
import com.dianjinshou.modules.recap.dto.CreateFullAnalysisRequest;
import com.dianjinshou.modules.recap.entity.AnalysisTask;
import com.dianjinshou.modules.recap.entity.AsrParagraph;
import com.dianjinshou.modules.recap.entity.Keyword;
import com.dianjinshou.modules.recap.entity.OptimizationAction;
import com.dianjinshou.modules.recap.entity.RecapNote;
import com.dianjinshou.modules.recap.mapper.AnalysisTaskMapper;
import com.dianjinshou.modules.recap.mapper.AsrParagraphMapper;
import com.dianjinshou.modules.recap.mapper.KeywordMapper;
import com.dianjinshou.modules.recap.mapper.OptimizationActionMapper;
import com.dianjinshou.modules.recap.mapper.RecapNoteMapper;
import com.dianjinshou.modules.recap.service.AnalysisService;
import com.dianjinshou.modules.recap.task.AnalysisTaskProducer;
import com.dianjinshou.modules.recap.vo.AnalysisTaskCreateVO;
import com.dianjinshou.modules.recap.vo.AnalysisTaskVO;
import com.dianjinshou.modules.recording.entity.Recording;
import com.dianjinshou.modules.recording.mapper.RecordingMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalysisIntegrationTest {

    @Mock private AnalysisTaskMapper analysisTaskMapper;
    @Mock private AsrParagraphMapper asrParagraphMapper;
    @Mock private KeywordMapper keywordMapper;
    @Mock private OptimizationActionMapper optimizationActionMapper;
    @Mock private RecapNoteMapper recapNoteMapper;
    @Mock private RecordingMapper recordingMapper;
    @Mock private AnalysisTaskProducer analysisTaskProducer;

    @InjectMocks
    private AnalysisService analysisService;

    @BeforeAll
    static void initCache() {
        MybatisConfiguration config = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(config, "");
        TableInfoHelper.initTableInfo(assistant, AnalysisTask.class);
        TableInfoHelper.initTableInfo(assistant, AsrParagraph.class);
        TableInfoHelper.initTableInfo(assistant, Keyword.class);
        TableInfoHelper.initTableInfo(assistant, OptimizationAction.class);
        TableInfoHelper.initTableInfo(assistant, RecapNote.class);
        TableInfoHelper.initTableInfo(assistant, Recording.class);
    }

    @BeforeEach
    void setUp() {
        setSecurityContext(1L, "operator", 5L);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void fullAnalysisFlow_create_query_result() {
        // 1. Create full analysis task
        Recording recording = new Recording();
        recording.setId(10L);
        recording.setOrgId(5L);
        recording.setUserId(1L);
        when(recordingMapper.selectById(10L)).thenReturn(recording);
        when(analysisTaskMapper.insert(any(AnalysisTask.class))).thenReturn(1);

        CreateFullAnalysisRequest req = new CreateFullAnalysisRequest();
        req.setRecordingId(10L);
        req.setIndustry("电商");

        AnalysisTaskCreateVO createVO = analysisService.createFullAnalysis(req);

        assertNotNull(createVO);
        assertEquals("pending", createVO.getStatus());
        verify(analysisTaskProducer).send(any());

        // 2. Query task detail (simulate status transition to completed)
        AnalysisTask completedTask = new AnalysisTask();
        completedTask.setId(1L);
        completedTask.setRecordingId(10L);
        completedTask.setUserId(1L);
        completedTask.setOrgId(5L);
        completedTask.setType("full");
        completedTask.setStatus("completed");
        completedTask.setAiModel("doubao");
        completedTask.setAsrWordCount(5000);
        completedTask.setConsumedChars(8000L);

        when(analysisTaskMapper.selectById(1L)).thenReturn(completedTask);

        AnalysisTaskVO detailVO = analysisService.detail(1L);

        assertEquals("completed", detailVO.getStatus());
        assertEquals("full", detailVO.getType());
        assertEquals(5000, detailVO.getAsrWordCount());
    }

    @Test
    void clipAnalysisFlow_create_withValidation() {
        Recording recording = new Recording();
        recording.setId(10L);
        recording.setOrgId(5L);
        recording.setUserId(1L);
        when(recordingMapper.selectById(10L)).thenReturn(recording);
        when(analysisTaskMapper.insert(any(AnalysisTask.class))).thenReturn(1);

        CreateClipAnalysisRequest req = new CreateClipAnalysisRequest();
        req.setRecordingId(10L);
        req.setClipCategory("RETENTION");
        req.setClipStart(60);
        req.setClipEnd(180);
        req.setClipFilename("片段一");

        AnalysisTaskCreateVO vo = analysisService.createClipAnalysis(req);

        assertNotNull(vo);
        assertEquals("pending", vo.getStatus());
    }

    @Test
    void clipAnalysis_invalidTimeRange_rejected() {
        Recording recording = new Recording();
        recording.setId(10L);
        recording.setOrgId(5L);
        recording.setUserId(1L);
        when(recordingMapper.selectById(10L)).thenReturn(recording);

        CreateClipAnalysisRequest req = new CreateClipAnalysisRequest();
        req.setRecordingId(10L);
        req.setClipCategory("RETENTION");
        req.setClipStart(180);
        req.setClipEnd(60); // end before start!

        assertThrows(BusinessException.class, () -> analysisService.createClipAnalysis(req));
    }

    @Test
    void createAnalysis_recordingNotFound_rejected() {
        when(recordingMapper.selectById(999L)).thenReturn(null);

        CreateFullAnalysisRequest req = new CreateFullAnalysisRequest();
        req.setRecordingId(999L);

        assertThrows(BusinessException.class, () -> analysisService.createFullAnalysis(req));
    }

    @Test
    void createAnalysis_crossOrgRecording_rejected() {
        Recording recording = new Recording();
        recording.setId(10L);
        recording.setOrgId(99L); // different org
        recording.setUserId(2L);
        when(recordingMapper.selectById(10L)).thenReturn(recording);

        CreateFullAnalysisRequest req = new CreateFullAnalysisRequest();
        req.setRecordingId(10L);

        assertThrows(BusinessException.class, () -> analysisService.createFullAnalysis(req));
    }

    @Test
    void detail_taskNotFound_rejected() {
        when(analysisTaskMapper.selectById(999L)).thenReturn(null);
        assertThrows(BusinessException.class, () -> analysisService.detail(999L));
    }

    @Test
    void detail_crossOrgTask_rejected() {
        AnalysisTask task = new AnalysisTask();
        task.setId(1L);
        task.setUserId(2L);
        task.setOrgId(99L); // different org
        task.setType("full");
        task.setStatus("completed");
        when(analysisTaskMapper.selectById(1L)).thenReturn(task);

        assertThrows(BusinessException.class, () -> analysisService.detail(1L));
    }

    private void setSecurityContext(Long userId, String role, Long orgId) {
        SecurityUser su = new SecurityUser(userId, role, orgId);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(su, null, Collections.emptyList()));
    }
}
