package com.dianjinshou.modules.recap.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dianjinshou.common.enums.AnalysisStatus;
import com.dianjinshou.common.enums.ClipCategory;
import com.dianjinshou.common.enums.RecapType;
import com.dianjinshou.common.exception.BusinessException;
import com.dianjinshou.common.response.ErrorCode;
import com.dianjinshou.common.response.PageResult;
import com.dianjinshou.common.security.SecurityUser;
import com.dianjinshou.modules.recap.dto.CreateClipAnalysisRequest;
import com.dianjinshou.modules.recap.dto.CreateFullAnalysisRequest;
import com.dianjinshou.modules.recap.dto.SaveNoteRequest;
import com.dianjinshou.modules.recap.dto.SaveOptimizationRequest;
import com.dianjinshou.modules.recap.entity.AnalysisTask;
import com.dianjinshou.modules.recap.entity.AsrParagraph;
import com.dianjinshou.modules.recap.entity.Keyword;
import com.dianjinshou.modules.recap.entity.RecapNote;
import com.dianjinshou.modules.recap.mapper.AnalysisTaskMapper;
import com.dianjinshou.modules.recap.mapper.AsrParagraphMapper;
import com.dianjinshou.modules.recap.mapper.KeywordMapper;
import com.dianjinshou.modules.recap.mapper.OptimizationActionMapper;
import com.dianjinshou.modules.recap.mapper.RecapNoteMapper;
import com.dianjinshou.modules.recap.vo.AnalysisTaskCreateVO;
import com.dianjinshou.modules.recap.vo.AnalysisTaskVO;
import com.dianjinshou.modules.recap.vo.AsrParagraphVO;
import com.dianjinshou.modules.recap.vo.KeywordListVO;
import com.dianjinshou.modules.recap.vo.NoteVO;
import com.dianjinshou.modules.recap.task.AnalysisTaskProducer;
import com.dianjinshou.modules.recording.entity.Recording;
import com.dianjinshou.modules.recording.mapper.RecordingMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalysisServiceTest {

    @Mock
    private AnalysisTaskMapper analysisTaskMapper;
    @Mock
    private AsrParagraphMapper asrParagraphMapper;
    @Mock
    private KeywordMapper keywordMapper;
    @Mock
    private OptimizationActionMapper optimizationActionMapper;
    @Mock
    private RecapNoteMapper recapNoteMapper;
    @Mock
    private RecordingMapper recordingMapper;
    @Mock
    private AnalysisTaskProducer analysisTaskProducer;

    @InjectMocks
    private AnalysisService analysisService;

    @BeforeAll
    static void initTableInfo() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), Recording.class);
    }

    @BeforeEach
    void setUp() {
        SecurityUser user = new SecurityUser(1L, "admin", 5L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createFullAnalysis_success() {
        Recording recording = buildRecording(10L, 5L);
        when(recordingMapper.selectById(10L)).thenReturn(recording);
        when(analysisTaskMapper.insert(any(AnalysisTask.class))).thenReturn(1);

        CreateFullAnalysisRequest req = new CreateFullAnalysisRequest();
        req.setRecordingId(10L);
        req.setIndustry("知识付费");
        req.setAiModel("doubao");

        AnalysisTaskCreateVO result = analysisService.createFullAnalysis(req);

        assertNotNull(result);
        assertEquals(AnalysisStatus.PENDING.getCode(), result.getStatus());
        verify(analysisTaskMapper).insert(any(AnalysisTask.class));
    }

    @Test
    void createFullAnalysis_recordingNotFound() {
        when(recordingMapper.selectById(999L)).thenReturn(null);

        CreateFullAnalysisRequest req = new CreateFullAnalysisRequest();
        req.setRecordingId(999L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> analysisService.createFullAnalysis(req));
        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void createClipAnalysis_success() {
        Recording recording = buildRecording(10L, 5L);
        when(recordingMapper.selectById(10L)).thenReturn(recording);
        when(analysisTaskMapper.insert(any(AnalysisTask.class))).thenReturn(1);

        CreateClipAnalysisRequest req = new CreateClipAnalysisRequest();
        req.setRecordingId(10L);
        req.setClipStart(15);
        req.setClipEnd(47);
        req.setClipCategory(ClipCategory.QUALITY_SPEECH.getCode());

        AnalysisTaskCreateVO result = analysisService.createClipAnalysis(req);

        assertNotNull(result);
        assertEquals(AnalysisStatus.PENDING.getCode(), result.getStatus());
    }

    @Test
    void createClipAnalysis_invalidCategory() {
        Recording recording = buildRecording(10L, 5L);
        when(recordingMapper.selectById(10L)).thenReturn(recording);

        CreateClipAnalysisRequest req = new CreateClipAnalysisRequest();
        req.setRecordingId(10L);
        req.setClipStart(15);
        req.setClipEnd(47);
        req.setClipCategory("INVALID_CATEGORY");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> analysisService.createClipAnalysis(req));
        assertEquals(ErrorCode.PARAM_ERROR, ex.getErrorCode());
    }

    @Test
    void createClipAnalysis_invalidTimeRange() {
        Recording recording = buildRecording(10L, 5L);
        when(recordingMapper.selectById(10L)).thenReturn(recording);

        CreateClipAnalysisRequest req = new CreateClipAnalysisRequest();
        req.setRecordingId(10L);
        req.setClipStart(50);
        req.setClipEnd(30);
        req.setClipCategory(ClipCategory.RETENTION.getCode());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> analysisService.createClipAnalysis(req));
        assertEquals(ErrorCode.PARAM_ERROR, ex.getErrorCode());
    }

    @Test
    void detail_success() {
        AnalysisTask task = buildTask(1L, 5L);
        when(analysisTaskMapper.selectById(1L)).thenReturn(task);

        AnalysisTaskVO vo = analysisService.detail(1L);

        assertNotNull(vo);
        assertEquals(1L, vo.getId());
        assertEquals(RecapType.FULL.getCode(), vo.getType());
        assertEquals(AnalysisStatus.PENDING.getCode(), vo.getStatus());
    }

    @Test
    void detail_notFound() {
        when(analysisTaskMapper.selectById(999L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> analysisService.detail(999L));
        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void detail_crossOrgAccess() {
        AnalysisTask task = buildTask(1L, 99L); // different org
        when(analysisTaskMapper.selectById(1L)).thenReturn(task);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> analysisService.detail(1L));
        assertEquals(ErrorCode.CROSS_ORG_ACCESS, ex.getErrorCode());
    }

    @Test
    void getParagraphs_success() {
        AnalysisTask task = buildTask(1L, 5L);
        when(analysisTaskMapper.selectById(1L)).thenReturn(task);

        AsrParagraph p1 = buildParagraph(1L, 1L, 0);
        AsrParagraph p2 = buildParagraph(2L, 1L, 1);

        Page<AsrParagraph> mockPage = new Page<>(1, 100);
        mockPage.setRecords(Arrays.asList(p1, p2));
        mockPage.setTotal(2);

        when(asrParagraphMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenReturn(mockPage);

        PageResult<AsrParagraphVO> result = analysisService.getParagraphs(1L, 1, 100);

        assertNotNull(result);
        assertEquals(2, result.getItems().size());
        assertEquals(0, result.getItems().get(0).getParagraphIndex());
    }

    @Test
    void getKeywords_success() {
        AnalysisTask task = buildTask(1L, 5L);
        when(analysisTaskMapper.selectById(1L)).thenReturn(task);

        Keyword k1 = buildKeyword(1L, 1L, "operational", "互动力");
        Keyword k2 = buildKeyword(2L, 1L, "sensitive", "违规词");

        Page<Keyword> mockPage = new Page<>(1, 20);
        mockPage.setRecords(Arrays.asList(k1));
        mockPage.setTotal(1);

        when(keywordMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenReturn(mockPage);
        when(keywordMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Arrays.asList(k1, k2));

        KeywordListVO result = analysisService.getKeywords(1L, "operational", null, 1, 20);

        assertNotNull(result);
        assertEquals(1, result.getItems().size());
        assertEquals(1, result.getStats().get("totalOperational").intValue());
        assertEquals(1, result.getStats().get("totalSensitive").intValue());
    }

    @Test
    void saveOptimization_success() {
        AnalysisTask task = buildTask(1L, 5L);
        when(analysisTaskMapper.selectById(1L)).thenReturn(task);
        when(analysisTaskMapper.updateById(any(AnalysisTask.class))).thenReturn(1);
        when(optimizationActionMapper.insert(any())).thenReturn(1);

        SaveOptimizationRequest req = new SaveOptimizationRequest();
        req.setAction("增加互动话术");
        req.setGoal("提升互动率到15%");

        analysisService.saveOptimization(1L, req);

        verify(analysisTaskMapper).updateById(any(AnalysisTask.class));
        verify(optimizationActionMapper).insert(any());
    }

    @Test
    void getNotes_existing() {
        AnalysisTask task = buildTask(1L, 5L);
        when(analysisTaskMapper.selectById(1L)).thenReturn(task);

        RecapNote note = new RecapNote();
        note.setId(10L);
        note.setTaskId(1L);
        note.setTabType("MINUTE_SEGMENTS");
        note.setContentHtml("<p>笔记内容</p>");

        when(recapNoteMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(note);

        NoteVO result = analysisService.getNotes(1L, "MINUTE_SEGMENTS");

        assertNotNull(result);
        assertEquals("MINUTE_SEGMENTS", result.getTabType());
        assertEquals("<p>笔记内容</p>", result.getContentHtml());
    }

    @Test
    void getNotes_empty() {
        AnalysisTask task = buildTask(1L, 5L);
        when(analysisTaskMapper.selectById(1L)).thenReturn(task);
        when(recapNoteMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        NoteVO result = analysisService.getNotes(1L, "MINUTE_SEGMENTS");

        assertNotNull(result);
        assertEquals("MINUTE_SEGMENTS", result.getTabType());
        assertEquals("", result.getContentHtml());
    }

    @Test
    void saveNotes_insert() {
        AnalysisTask task = buildTask(1L, 5L);
        when(analysisTaskMapper.selectById(1L)).thenReturn(task);
        when(recapNoteMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(recapNoteMapper.insert(any(RecapNote.class))).thenReturn(1);

        SaveNoteRequest req = new SaveNoteRequest();
        req.setTabType("AI_SCRIPT");
        req.setContentHtml("<p>新笔记</p>");

        NoteVO result = analysisService.saveNotes(1L, req);

        assertNotNull(result);
        assertEquals("AI_SCRIPT", result.getTabType());
        verify(recapNoteMapper).insert(any(RecapNote.class));
    }

    @Test
    void saveNotes_update() {
        AnalysisTask task = buildTask(1L, 5L);
        when(analysisTaskMapper.selectById(1L)).thenReturn(task);

        RecapNote existing = new RecapNote();
        existing.setId(10L);
        existing.setTaskId(1L);
        existing.setTabType("AI_SCRIPT");
        existing.setContentHtml("<p>旧笔记</p>");

        when(recapNoteMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);
        when(recapNoteMapper.updateById(any(RecapNote.class))).thenReturn(1);

        SaveNoteRequest req = new SaveNoteRequest();
        req.setTabType("AI_SCRIPT");
        req.setContentHtml("<p>更新笔记</p>");

        NoteVO result = analysisService.saveNotes(1L, req);

        assertNotNull(result);
        verify(recapNoteMapper).updateById(any(RecapNote.class));
    }

    @Test
    void reAnalyze_success() {
        AnalysisTask task = buildTask(1L, 5L);
        task.setStatus(AnalysisStatus.FAILED.getCode());
        when(analysisTaskMapper.selectById(1L)).thenReturn(task);
        when(analysisTaskMapper.updateById(any(AnalysisTask.class))).thenReturn(1);

        AnalysisTaskCreateVO result = analysisService.reAnalyze(1L);

        assertNotNull(result);
        assertEquals(AnalysisStatus.PENDING.getCode(), result.getStatus());
    }

    @Test
    void cancel_success() {
        AnalysisTask task = buildTask(1L, 5L);
        task.setStatus(AnalysisStatus.ASR_PROCESSING.getCode());
        when(analysisTaskMapper.selectById(1L)).thenReturn(task);
        when(analysisTaskMapper.updateById(any(AnalysisTask.class))).thenReturn(1);

        analysisService.cancel(1L);

        verify(analysisTaskMapper).updateById(any(AnalysisTask.class));
    }

    @Test
    void cancel_alreadyCompleted() {
        AnalysisTask task = buildTask(1L, 5L);
        task.setStatus(AnalysisStatus.COMPLETED.getCode());
        when(analysisTaskMapper.selectById(1L)).thenReturn(task);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> analysisService.cancel(1L));
        assertEquals(ErrorCode.PARAM_ERROR, ex.getErrorCode());
    }

    // ========== QA 增量: 边界测试 ==========

    @Test
    void createFullAnalysis_superAdmin_crossOrg_success() {
        SecurityContextHolder.clearContext();
        SecurityUser superAdmin = new SecurityUser(1L, "super_admin", 5L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(superAdmin, null, Collections.emptyList()));

        Recording recording = buildRecording(10L, 999L); // different org
        when(recordingMapper.selectById(10L)).thenReturn(recording);
        when(analysisTaskMapper.insert(any(AnalysisTask.class))).thenReturn(1);

        CreateFullAnalysisRequest req = new CreateFullAnalysisRequest();
        req.setRecordingId(10L);

        AnalysisTaskCreateVO result = analysisService.createFullAnalysis(req);
        assertNotNull(result);
    }

    @Test
    void createFullAnalysis_aiModelDefaultsToDoubao() {
        Recording recording = buildRecording(10L, 5L);
        when(recordingMapper.selectById(10L)).thenReturn(recording);
        when(analysisTaskMapper.insert(any(AnalysisTask.class))).thenAnswer(inv -> {
            AnalysisTask t = inv.getArgument(0);
            assertEquals("doubao", t.getAiModel());
            return 1;
        });

        CreateFullAnalysisRequest req = new CreateFullAnalysisRequest();
        req.setRecordingId(10L);
        // aiModel not set

        analysisService.createFullAnalysis(req);
        verify(analysisTaskMapper).insert(any());
    }

    @Test
    void createFullAnalysis_specifiedAiModel() {
        Recording recording = buildRecording(10L, 5L);
        when(recordingMapper.selectById(10L)).thenReturn(recording);
        when(analysisTaskMapper.insert(any(AnalysisTask.class))).thenAnswer(inv -> {
            AnalysisTask t = inv.getArgument(0);
            assertEquals("deepseek_r1", t.getAiModel());
            return 1;
        });

        CreateFullAnalysisRequest req = new CreateFullAnalysisRequest();
        req.setRecordingId(10L);
        req.setAiModel("deepseek_r1");

        analysisService.createFullAnalysis(req);
    }

    @Test
    void createClipAnalysis_clipStartEqualsClipEnd_throws() {
        Recording recording = buildRecording(10L, 5L);
        when(recordingMapper.selectById(10L)).thenReturn(recording);

        CreateClipAnalysisRequest req = new CreateClipAnalysisRequest();
        req.setRecordingId(10L);
        req.setClipStart(60);
        req.setClipEnd(60);
        req.setClipCategory(ClipCategory.RETENTION.getCode());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> analysisService.createClipAnalysis(req));
        assertEquals(ErrorCode.PARAM_ERROR, ex.getErrorCode());
    }

    @Test
    void cancel_pendingTask_success() {
        AnalysisTask task = buildTask(1L, 5L);
        task.setStatus(AnalysisStatus.PENDING.getCode());
        when(analysisTaskMapper.selectById(1L)).thenReturn(task);
        when(analysisTaskMapper.updateById(any(AnalysisTask.class))).thenReturn(1);

        analysisService.cancel(1L);
        verify(analysisTaskMapper).updateById(any(AnalysisTask.class));
    }

    @Test
    void cancel_alreadyFailed_throws() {
        AnalysisTask task = buildTask(1L, 5L);
        task.setStatus(AnalysisStatus.FAILED.getCode());
        when(analysisTaskMapper.selectById(1L)).thenReturn(task);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> analysisService.cancel(1L));
        assertEquals(ErrorCode.PARAM_ERROR, ex.getErrorCode());
    }

    @Test
    void reAnalyze_notFound_throws() {
        when(analysisTaskMapper.selectById(999L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> analysisService.reAnalyze(999L));
        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void listTasks_success() {
        Page<AnalysisTask> page = new Page<>(1, 10);
        page.setRecords(Collections.singletonList(buildTask(1L, 5L)));
        page.setTotal(1);
        when(analysisTaskMapper.selectPage(any(), any())).thenReturn(page);

        PageResult<AnalysisTaskVO> result = analysisService.listTasks(null, null, null, 1, 10);
        assertNotNull(result);
        assertEquals(1, result.getTotal());
    }

    @Test
    void listTasks_withStreamerId_noRecordings_returnsEmpty() {
        when(recordingMapper.selectList(any())).thenReturn(new ArrayList<>());

        PageResult<AnalysisTaskVO> result = analysisService.listTasks(null, null, 99L, 1, 10);
        assertEquals(0, result.getTotal());
        assertTrue(result.getItems().isEmpty());
    }

    private Recording buildRecording(Long id, Long orgId) {
        Recording r = new Recording();
        r.setId(id);
        r.setOrgId(orgId);
        r.setUserId(1L);
        r.setStatus("completed");
        r.setAnalysisStatus("none");
        r.setCreatedAt(LocalDateTime.of(2025, 1, 1, 10, 0));
        return r;
    }

    private AnalysisTask buildTask(Long id, Long orgId) {
        AnalysisTask task = new AnalysisTask();
        task.setId(id);
        task.setRecordingId(10L);
        task.setUserId(1L);
        task.setOrgId(orgId);
        task.setType(RecapType.FULL.getCode());
        task.setStatus(AnalysisStatus.PENDING.getCode());
        task.setPriority(5);
        task.setAiModel("doubao");
        task.setAsrWordCount(0);
        task.setSensitiveCount(0);
        task.setConsumedChars(0L);
        task.setCreatedAt(LocalDateTime.of(2025, 1, 1, 10, 0));
        return task;
    }

    private AsrParagraph buildParagraph(Long id, Long taskId, int index) {
        AsrParagraph p = new AsrParagraph();
        p.setId(id);
        p.setTaskId(taskId);
        p.setParagraphIndex(index);
        p.setStartTime("00:0" + index + ":00");
        p.setTextContent("测试段落" + index);
        p.setWordCount(20);
        p.setWordsPerMin(40);
        p.setOnlineCount(100);
        p.setBarrageCount(5);
        p.setTransactionCount(1);
        p.setInteractionRate(new BigDecimal("12.50"));
        p.setTransactionRate(new BigDecimal("0.50"));
        p.setSalesAmount(new BigDecimal("100.00"));
        p.setUvValue(new BigDecimal("5.00"));
        return p;
    }

    private Keyword buildKeyword(Long id, Long taskId, String type, String category) {
        Keyword k = new Keyword();
        k.setId(id);
        k.setTaskId(taskId);
        k.setType(type);
        k.setCategory(category);
        k.setWord("测试词");
        k.setHitCountVideo1(3);
        k.setTotalCount(3);
        k.setSource("system");
        return k;
    }
}
