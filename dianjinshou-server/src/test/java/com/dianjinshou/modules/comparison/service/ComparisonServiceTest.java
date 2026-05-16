package com.dianjinshou.modules.comparison.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dianjinshou.common.exception.BusinessException;
import com.dianjinshou.common.response.ErrorCode;
import com.dianjinshou.common.response.PageResult;
import com.dianjinshou.common.security.SecurityUser;
import com.dianjinshou.modules.comparison.dto.CreateComparisonRequest;
import com.dianjinshou.modules.comparison.entity.Comparison;
import com.dianjinshou.modules.comparison.mapper.ComparisonMapper;
import com.dianjinshou.modules.comparison.vo.ComparisonVO;
import com.dianjinshou.modules.recap.entity.Keyword;
import com.dianjinshou.modules.recap.mapper.KeywordMapper;
import com.dianjinshou.modules.recording.entity.Recording;
import com.dianjinshou.modules.recording.mapper.RecordingMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ComparisonServiceTest {

    @Mock
    private ComparisonMapper comparisonMapper;
    @Mock
    private RecordingMapper recordingMapper;
    @Mock
    private KeywordMapper keywordMapper;

    @InjectMocks
    private ComparisonService comparisonService;

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
    void create_success() {
        Recording optimize = buildRecording(10L, 5L);
        Recording reference = buildRecording(20L, 5L);
        when(recordingMapper.selectById(10L)).thenReturn(optimize);
        when(recordingMapper.selectById(20L)).thenReturn(reference);
        when(comparisonMapper.insert(any(Comparison.class))).thenReturn(1);

        CreateComparisonRequest req = new CreateComparisonRequest();
        req.setRecordingIdOptimize(10L);
        req.setRecordingIdReference(20L);
        req.setType("full");

        ComparisonVO result = comparisonService.create(req);

        assertNotNull(result);
        assertEquals("full", result.getType());
        assertEquals("pending", result.getStatus());
        verify(comparisonMapper).insert(any(Comparison.class));
    }

    @Test
    void create_optimizeNotFound() {
        when(recordingMapper.selectById(10L)).thenReturn(null);

        CreateComparisonRequest req = new CreateComparisonRequest();
        req.setRecordingIdOptimize(10L);
        req.setRecordingIdReference(20L);
        req.setType("full");

        BusinessException ex = assertThrows(BusinessException.class, () -> comparisonService.create(req));
        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void create_crossOrgAccess() {
        Recording optimize = buildRecording(10L, 99L);
        when(recordingMapper.selectById(10L)).thenReturn(optimize);

        CreateComparisonRequest req = new CreateComparisonRequest();
        req.setRecordingIdOptimize(10L);
        req.setRecordingIdReference(20L);
        req.setType("full");

        BusinessException ex = assertThrows(BusinessException.class, () -> comparisonService.create(req));
        assertEquals(ErrorCode.CROSS_ORG_ACCESS, ex.getErrorCode());
    }

    @Test
    void detail_success() {
        Comparison comparison = buildComparison(1L, 5L);
        when(comparisonMapper.selectById(1L)).thenReturn(comparison);

        ComparisonVO result = comparisonService.detail(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void detail_notFound() {
        when(comparisonMapper.selectById(1L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class, () -> comparisonService.detail(1L));
        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void swap_success() {
        Comparison comparison = buildComparison(1L, 5L);
        comparison.setRecordingIdOptimize(10L);
        comparison.setRecordingIdReference(20L);
        comparison.setTaskIdOptimize(100L);
        comparison.setTaskIdReference(200L);
        when(comparisonMapper.selectById(1L)).thenReturn(comparison);
        when(comparisonMapper.updateById(any(Comparison.class))).thenReturn(1);

        ComparisonVO result = comparisonService.swap(1L);

        assertEquals(20L, result.getRecordingIdOptimize());
        assertEquals(10L, result.getRecordingIdReference());
        assertEquals(200L, result.getTaskIdOptimize());
        assertEquals(100L, result.getTaskIdReference());
    }

    @Test
    void batchDelete_success() {
        Comparison c1 = buildComparison(1L, 5L);
        Comparison c2 = buildComparison(2L, 5L);
        when(comparisonMapper.selectById(1L)).thenReturn(c1);
        when(comparisonMapper.selectById(2L)).thenReturn(c2);
        when(comparisonMapper.deleteById(any(Long.class))).thenReturn(1);

        comparisonService.batchDelete(Arrays.asList(1L, 2L));

        verify(comparisonMapper, times(2)).deleteById(any(Long.class));
    }

    @Test
    void list_success() {
        Page<Comparison> mockPage = new Page<>(1, 10);
        mockPage.setRecords(Collections.singletonList(buildComparison(1L, 5L)));
        mockPage.setTotal(1);
        when(comparisonMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(mockPage);

        PageResult<ComparisonVO> result = comparisonService.list("full", 1, 10);

        assertEquals(1, result.getTotal());
        assertEquals(1, result.getItems().size());
    }

    @Test
    void getKeywords_notFound() {
        when(comparisonMapper.selectById(1L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class, () -> comparisonService.getKeywords(1L));
        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void getKeywords_success() {
        Comparison comparison = buildComparison(1L, 5L);
        when(comparisonMapper.selectById(1L)).thenReturn(comparison);
        when(keywordMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

        List<Keyword> result = comparisonService.getKeywords(1L);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // --- Helpers ---

    private Recording buildRecording(Long id, Long orgId) {
        Recording r = new Recording();
        r.setId(id);
        r.setUserId(1L);
        r.setOrgId(orgId);
        return r;
    }

    private Comparison buildComparison(Long id, Long orgId) {
        Comparison c = new Comparison();
        c.setId(id);
        c.setUserId(1L);
        c.setOrgId(orgId);
        c.setType("full");
        c.setStatus("pending");
        c.setRecordingIdOptimize(10L);
        c.setRecordingIdReference(20L);
        return c;
    }
}
