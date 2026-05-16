package com.dianjinshou.modules.recording.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dianjinshou.common.exception.BusinessException;
import com.dianjinshou.common.response.ErrorCode;
import com.dianjinshou.common.response.PageResult;
import com.dianjinshou.common.security.SecurityUser;
import com.dianjinshou.modules.recording.dto.RecordingQueryRequest;
import com.dianjinshou.modules.recording.dto.RenameRequest;
import com.dianjinshou.modules.recording.entity.Recording;
import com.dianjinshou.modules.recording.mapper.RecordingMapper;
import com.dianjinshou.modules.recording.vo.RecordingListVO;
import com.dianjinshou.modules.recording.vo.RecordingVO;
import com.dianjinshou.modules.streamer.entity.Streamer;
import com.dianjinshou.modules.recap.mapper.AnalysisTaskMapper;
import com.dianjinshou.modules.recap.task.AnalysisTaskProducer;
import com.dianjinshou.modules.streamer.mapper.StreamerMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

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
class RecordingServiceTest {

    @Mock
    private RecordingMapper recordingMapper;

    @Mock
    private StreamerMapper streamerMapper;

    @Mock
    private AnalysisTaskMapper analysisTaskMapper;

    @Mock
    private AnalysisTaskProducer analysisTaskProducer;

    @InjectMocks
    private RecordingService recordingService;

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
    void list_returnsPageResult() {
        Recording r1 = buildRecording(1L, 5L);
        Recording r2 = buildRecording(2L, 5L);

        Page<Recording> mockPage = new Page<>(1, 10);
        mockPage.setRecords(Arrays.asList(r1, r2));
        mockPage.setTotal(2);

        when(recordingMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenReturn(mockPage);

        RecordingQueryRequest req = new RecordingQueryRequest();
        PageResult<RecordingListVO> result = recordingService.list(req);

        assertNotNull(result);
        assertEquals(2, result.getItems().size());
        assertEquals(2, result.getTotal());
        assertEquals(1, result.getPage());
        assertEquals(10, result.getSize());
    }

    @Test
    void detail_returnsRecordingVO() {
        Recording recording = buildRecording(1L, 5L);
        recording.setStreamerId(10L);

        Streamer streamer = new Streamer();
        streamer.setId(10L);
        streamer.setAnchorName("TestAnchor");

        when(recordingMapper.selectById(1L)).thenReturn(recording);
        when(streamerMapper.selectById(10L)).thenReturn(streamer);

        RecordingVO vo = recordingService.detail(1L);

        assertNotNull(vo);
        assertEquals(1L, vo.getId());
        assertNotNull(vo.getStreamerInfo());
        assertEquals("TestAnchor", vo.getStreamerInfo().getAnchorName());
    }

    @Test
    void rename_success() {
        Recording recording = buildRecording(1L, 5L);
        recording.setLocalFileName("old-name");

        when(recordingMapper.selectById(1L)).thenReturn(recording);
        when(recordingMapper.updateById(any(Recording.class))).thenReturn(1);

        RenameRequest req = new RenameRequest();
        req.setName("new-name");

        RecordingVO vo = recordingService.rename(1L, req);

        assertNotNull(vo);
        assertEquals("new-name", vo.getLocalFileName());
        verify(recordingMapper).updateById(any(Recording.class));
    }

    @Test
    void rename_tooLong() {
        // 16 Unicode code points (Chinese characters count as 1 code point each)
        String longName = "一二三四五六七八九十壹贰叁肆伍陆";

        RenameRequest req = new RenameRequest();
        req.setName(longName);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> recordingService.rename(1L, req));
        assertEquals(ErrorCode.PARAM_ERROR, ex.getErrorCode());
    }

    @Test
    void batchDelete_success() {
        Recording r1 = buildRecording(1L, 5L);
        Recording r2 = buildRecording(2L, 5L);

        when(recordingMapper.selectById(1L)).thenReturn(r1);
        when(recordingMapper.selectById(2L)).thenReturn(r2);
        when(recordingMapper.deleteById(1L)).thenReturn(1);
        when(recordingMapper.deleteById(2L)).thenReturn(1);

        int count = recordingService.batchDelete(Arrays.asList(1L, 2L));

        assertEquals(2, count);
        verify(recordingMapper).deleteById(1L);
        verify(recordingMapper).deleteById(2L);
    }

    // ========== QA 增量: 边界测试 ==========

    @Test
    void list_withStreamerIdFilter() {
        Recording r = buildRecording(1L, 5L);
        r.setStreamerId(10L);
        Page<Recording> mockPage = new Page<>(1, 10);
        mockPage.setRecords(Arrays.asList(r));
        mockPage.setTotal(1);
        when(recordingMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenReturn(mockPage);

        RecordingQueryRequest req = new RecordingQueryRequest();
        req.setStreamerId(10L);
        PageResult<RecordingListVO> result = recordingService.list(req);
        assertEquals(1, result.getTotal());
    }

    @Test
    void list_withStatusFilter() {
        Page<Recording> mockPage = new Page<>(1, 10);
        mockPage.setRecords(new ArrayList<>());
        mockPage.setTotal(0);
        when(recordingMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenReturn(mockPage);

        RecordingQueryRequest req = new RecordingQueryRequest();
        req.setStatus("completed");
        PageResult<RecordingListVO> result = recordingService.list(req);
        assertNotNull(result);
    }

    @Test
    void list_withAnalysisStatusFilter() {
        Page<Recording> mockPage = new Page<>(1, 10);
        mockPage.setRecords(new ArrayList<>());
        mockPage.setTotal(0);
        when(recordingMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenReturn(mockPage);

        RecordingQueryRequest req = new RecordingQueryRequest();
        req.setAnalysisStatus("completed");
        PageResult<RecordingListVO> result = recordingService.list(req);
        assertNotNull(result);
    }

    @Test
    void list_withDateRangeFilter() {
        Page<Recording> mockPage = new Page<>(1, 10);
        mockPage.setRecords(new ArrayList<>());
        mockPage.setTotal(0);
        when(recordingMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenReturn(mockPage);

        RecordingQueryRequest req = new RecordingQueryRequest();
        req.setStartDate("2026-01-01");
        req.setEndDate("2026-12-31");
        PageResult<RecordingListVO> result = recordingService.list(req);
        assertNotNull(result);
    }

    @Test
    void list_withTabFilter() {
        Page<Recording> mockPage = new Page<>(1, 10);
        mockPage.setRecords(new ArrayList<>());
        mockPage.setTotal(0);
        when(recordingMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenReturn(mockPage);

        RecordingQueryRequest req = new RecordingQueryRequest();
        req.setTab("COMPLETED");
        PageResult<RecordingListVO> result = recordingService.list(req);
        assertNotNull(result);
    }

    @Test
    void detail_crossOrg_throws() {
        Recording recording = buildRecording(1L, 999L); // different org
        when(recordingMapper.selectById(1L)).thenReturn(recording);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> recordingService.detail(1L));
        assertEquals(ErrorCode.CROSS_ORG_ACCESS, ex.getErrorCode());
    }

    @Test
    void rename_exactly15Chars_success() {
        Recording recording = buildRecording(1L, 5L);
        when(recordingMapper.selectById(1L)).thenReturn(recording);
        when(recordingMapper.updateById(any(Recording.class))).thenReturn(1);

        RenameRequest req = new RenameRequest();
        req.setName("一二三四五六七八九十壹贰叁肆伍"); // exactly 15 Chinese chars

        RecordingVO vo = recordingService.rename(1L, req);
        assertEquals("一二三四五六七八九十壹贰叁肆伍", vo.getLocalFileName());
    }

    @Test
    void rename_chinese16Chars_throws() {
        RenameRequest req = new RenameRequest();
        req.setName("一二三四五六七八九十壹贰叁肆伍陆"); // 16 Chinese chars

        BusinessException ex = assertThrows(BusinessException.class,
                () -> recordingService.rename(1L, req));
        assertEquals(ErrorCode.PARAM_ERROR, ex.getErrorCode());
    }

    @Test
    void rename_notFound_throws() {
        when(recordingMapper.selectById(999L)).thenReturn(null);

        RenameRequest req = new RenameRequest();
        req.setName("新名称");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> recordingService.rename(999L, req));
        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void export_success() {
        Recording recording = buildRecording(1L, 5L);
        when(recordingMapper.selectById(1L)).thenReturn(recording);

        java.util.Map<String, Object> result = recordingService.export(1L);
        assertNotNull(result);
        assertTrue(result.containsKey("message"));
        assertEquals("导出功能开发中", result.get("message"));
    }

    @Test
    void export_notFound_throws() {
        when(recordingMapper.selectById(999L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> recordingService.export(999L));
        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void batchDelete_nullIds_throws() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> recordingService.batchDelete(null));
        assertEquals(ErrorCode.PARAM_ERROR, ex.getErrorCode());
    }

    @Test
    void batchDelete_emptyIds_throws() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> recordingService.batchDelete(new ArrayList<>()));
        assertEquals(ErrorCode.PARAM_ERROR, ex.getErrorCode());
    }

    @Test
    void batchDelete_partialNotFound_throws() {
        Recording r1 = buildRecording(1L, 5L);
        when(recordingMapper.selectById(1L)).thenReturn(r1);
        when(recordingMapper.selectById(999L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> recordingService.batchDelete(Arrays.asList(1L, 999L)));
        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void batchDelete_crossOrg_throws() {
        Recording r = buildRecording(1L, 999L); // different org
        when(recordingMapper.selectById(1L)).thenReturn(r);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> recordingService.batchDelete(Collections.singletonList(1L)));
        assertEquals(ErrorCode.CROSS_ORG_ACCESS, ex.getErrorCode());
    }

    private Recording buildRecording(Long id, Long orgId) {
        Recording r = new Recording();
        r.setId(id);
        r.setOrgId(orgId);
        r.setUserId(1L);
        r.setStatus("completed");
        r.setAnalysisStatus("none");
        r.setLocalFileName("test-recording");
        r.setStartTime(LocalDateTime.of(2025, 1, 1, 10, 0));
        r.setEndTime(LocalDateTime.of(2025, 1, 1, 11, 0));
        r.setDuration(3600);
        r.setFileSize(1024000L);
        r.setResolution("1920x1080");
        r.setCreatedAt(LocalDateTime.of(2025, 1, 1, 10, 0));
        return r;
    }
}
