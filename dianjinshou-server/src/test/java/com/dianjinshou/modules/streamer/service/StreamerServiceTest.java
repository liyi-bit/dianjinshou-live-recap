package com.dianjinshou.modules.streamer.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dianjinshou.common.enums.AccountType;
import com.dianjinshou.common.enums.Platform;
import com.dianjinshou.common.exception.BusinessException;
import com.dianjinshou.common.response.ErrorCode;
import com.dianjinshou.common.response.PageResult;
import com.dianjinshou.common.security.SecurityUser;
import com.dianjinshou.modules.streamer.dto.CreateStreamerRequest;
import com.dianjinshou.modules.streamer.dto.StreamerQueryRequest;
import com.dianjinshou.modules.streamer.dto.UpdateStreamerRequest;
import com.dianjinshou.modules.streamer.entity.Streamer;
import com.dianjinshou.modules.streamer.mapper.StreamerMapper;
import com.dianjinshou.modules.streamer.vo.StreamerListVO;
import com.dianjinshou.modules.streamer.vo.StreamerStatsVO;
import com.dianjinshou.modules.streamer.vo.StreamerVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StreamerServiceTest {

    @Mock
    private StreamerMapper streamerMapper;

    @InjectMocks
    private StreamerService streamerService;

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
        when(streamerMapper.insert(any(Streamer.class))).thenAnswer(invocation -> {
            Streamer s = invocation.getArgument(0);
            s.setId(100L);
            return 1;
        });

        CreateStreamerRequest req = new CreateStreamerRequest();
        req.setPlatform("douyin");
        req.setAnchorName("测试主播");
        req.setAccountType("own");

        StreamerVO result = streamerService.create(req);

        assertNotNull(result);
        assertEquals(100L, result.getId());
        assertEquals("测试主播", result.getAnchorName());
        assertEquals("douyin", result.getPlatform());
        assertEquals("own", result.getAccountType());
        verify(streamerMapper).insert(any(Streamer.class));
    }

    @Test
    void list_withFilters() {
        Streamer s1 = buildStreamer(1L, "主播A", Platform.DOUYIN, AccountType.OWN);
        List<Streamer> records = new ArrayList<>();
        records.add(s1);

        Page<Streamer> page = new Page<>(1, 10);
        page.setRecords(records);
        page.setTotal(1);

        when(streamerMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

        StreamerQueryRequest req = new StreamerQueryRequest();
        req.setKeyword("主播");
        req.setPlatform("douyin");
        req.setAccountType("own");
        req.setPage(1);
        req.setSize(10);

        PageResult<StreamerListVO> result = streamerService.list(req);

        assertNotNull(result);
        assertEquals(1, result.getTotal());
        assertEquals(1, result.getItems().size());
        assertEquals("主播A", result.getItems().get(0).getAnchorName());
    }

    @Test
    void detail_success() {
        Streamer streamer = buildStreamer(1L, "主播A", Platform.DOUYIN, AccountType.OWN);
        when(streamerMapper.selectById(1L)).thenReturn(streamer);

        StreamerVO result = streamerService.detail(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("主播A", result.getAnchorName());
    }

    @Test
    void detail_notFound() {
        when(streamerMapper.selectById(999L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> streamerService.detail(999L));
        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void update_success() {
        Streamer streamer = buildStreamer(1L, "主播A", Platform.DOUYIN, AccountType.OWN);
        when(streamerMapper.selectById(1L)).thenReturn(streamer);
        when(streamerMapper.updateById(any(Streamer.class))).thenReturn(1);

        UpdateStreamerRequest req = new UpdateStreamerRequest();
        req.setAnchorName("主播B");
        req.setAccountType("competitor");

        StreamerVO result = streamerService.update(1L, req);

        assertNotNull(result);
        assertEquals("主播B", result.getAnchorName());
        assertEquals("competitor", result.getAccountType());
        verify(streamerMapper).updateById(any(Streamer.class));
    }

    @Test
    void delete_success() {
        Streamer streamer = buildStreamer(1L, "主播A", Platform.DOUYIN, AccountType.OWN);
        when(streamerMapper.selectById(1L)).thenReturn(streamer);
        when(streamerMapper.deleteById(1L)).thenReturn(1);

        int deletedRecordings = streamerService.delete(1L);

        assertEquals(0, deletedRecordings);
        verify(streamerMapper).deleteById(1L);
    }

    @Test
    void startMonitor_success() {
        Streamer streamer = buildStreamer(1L, "主播A", Platform.DOUYIN, AccountType.OWN);
        streamer.setIsMonitoring(false);
        when(streamerMapper.selectById(1L)).thenReturn(streamer);
        when(streamerMapper.updateById(any(Streamer.class))).thenReturn(1);

        streamerService.startMonitor(1L);

        verify(streamerMapper).updateById(argThat(s -> Boolean.TRUE.equals(((Streamer) s).getIsMonitoring())));
    }

    @Test
    void stopMonitor_success() {
        Streamer streamer = buildStreamer(1L, "主播A", Platform.DOUYIN, AccountType.OWN);
        streamer.setIsMonitoring(true);
        when(streamerMapper.selectById(1L)).thenReturn(streamer);
        when(streamerMapper.updateById(any(Streamer.class))).thenReturn(1);

        streamerService.stopMonitor(1L);

        verify(streamerMapper).updateById(argThat(s -> Boolean.FALSE.equals(((Streamer) s).getIsMonitoring())));
    }

    @Test
    void stats_success() {
        List<Streamer> all = new ArrayList<>();
        all.add(buildStreamer(1L, "A", Platform.DOUYIN, AccountType.OWN));
        all.add(buildStreamer(2L, "B", Platform.KUAISHOU, AccountType.COMPETITOR));
        all.add(buildStreamer(3L, "C", Platform.SHIPINHAO, AccountType.INDUSTRY));

        Streamer monitoredStreamer = all.get(0);
        monitoredStreamer.setIsMonitoring(true);

        when(streamerMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(all);

        StreamerStatsVO result = streamerService.stats();

        assertNotNull(result);
        assertEquals(3, result.getTotal());
        assertEquals(1, result.getMonitoring());
        assertEquals(1, result.getOwnCount());
        assertEquals(1, result.getCompetitorCount());
        assertEquals(1, result.getIndustryCount());
    }

    // ========== QA 增量: 边界测试 ==========

    @Test
    void create_invalidPlatform_throws() {
        CreateStreamerRequest req = new CreateStreamerRequest();
        req.setPlatform("bilibili");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> streamerService.create(req));
        assertEquals(ErrorCode.PARAM_ERROR, ex.getErrorCode());
    }

    @Test
    void create_anchorNameFallbackToAccountId() {
        when(streamerMapper.insert(any(Streamer.class))).thenAnswer(invocation -> {
            Streamer s = invocation.getArgument(0);
            s.setId(101L);
            return 1;
        });

        CreateStreamerRequest req = new CreateStreamerRequest();
        req.setPlatform("douyin");
        req.setAccountId("dy_fallback");
        req.setAnchorName(null);

        StreamerVO result = streamerService.create(req);
        assertEquals("dy_fallback", result.getAnchorName());
    }

    @Test
    void create_anchorNameFallbackToDefault_whenBothNull() {
        when(streamerMapper.insert(any(Streamer.class))).thenAnswer(invocation -> {
            Streamer s = invocation.getArgument(0);
            s.setId(102L);
            return 1;
        });

        CreateStreamerRequest req = new CreateStreamerRequest();
        req.setPlatform("douyin");
        req.setAccountId(null);
        req.setAnchorName(null);

        StreamerVO result = streamerService.create(req);
        assertEquals("未命名主播", result.getAnchorName());
    }

    @Test
    void create_defaultLanguageFallback() {
        when(streamerMapper.insert(any(Streamer.class))).thenAnswer(invocation -> {
            Streamer s = invocation.getArgument(0);
            s.setId(103L);
            return 1;
        });

        CreateStreamerRequest req = new CreateStreamerRequest();
        req.setPlatform("douyin");
        req.setAccountId("dy123");
        req.setAnchorName("主播");
        // defaultLanguage not set

        StreamerVO result = streamerService.create(req);
        assertEquals("中文通用", result.getDefaultLanguage());
    }

    @Test
    void list_noFilter() {
        Page<Streamer> page = new Page<>(1, 10);
        page.setRecords(new ArrayList<>());
        page.setTotal(0);
        when(streamerMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

        StreamerQueryRequest req = new StreamerQueryRequest();
        PageResult<StreamerListVO> result = streamerService.list(req);

        assertNotNull(result);
        assertEquals(0, result.getTotal());
        assertEquals(1, result.getPage());
        assertEquals(10, result.getSize());
    }

    @Test
    void list_withAccountTypeFilter() {
        Page<Streamer> page = new Page<>(1, 10);
        page.setRecords(new ArrayList<>());
        page.setTotal(0);
        when(streamerMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

        StreamerQueryRequest req = new StreamerQueryRequest();
        req.setAccountType("own");
        PageResult<StreamerListVO> result = streamerService.list(req);
        assertNotNull(result);
    }

    @Test
    void list_withPlatformFilter() {
        Page<Streamer> page = new Page<>(1, 10);
        page.setRecords(new ArrayList<>());
        page.setTotal(0);
        when(streamerMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

        StreamerQueryRequest req = new StreamerQueryRequest();
        req.setPlatform("douyin");
        PageResult<StreamerListVO> result = streamerService.list(req);
        assertNotNull(result);
    }

    @Test
    void detail_crossOrg_throws() {
        Streamer streamer = buildStreamer(1L, "主播A", Platform.DOUYIN, AccountType.OWN);
        streamer.setOrgId(999L); // different org
        when(streamerMapper.selectById(1L)).thenReturn(streamer);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> streamerService.detail(1L));
        assertEquals(ErrorCode.CROSS_ORG_ACCESS, ex.getErrorCode());
    }

    @Test
    void detail_superAdmin_crossOrg_success() {
        SecurityContextHolder.clearContext();
        SecurityUser superAdmin = new SecurityUser(1L, "super_admin", 5L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(superAdmin, null, Collections.emptyList()));

        Streamer streamer = buildStreamer(1L, "主播A", Platform.DOUYIN, AccountType.OWN);
        streamer.setOrgId(999L);
        when(streamerMapper.selectById(1L)).thenReturn(streamer);

        StreamerVO result = streamerService.detail(1L);
        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void update_notFound_throws() {
        when(streamerMapper.selectById(999L)).thenReturn(null);

        UpdateStreamerRequest req = new UpdateStreamerRequest();
        req.setAnchorName("新名称");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> streamerService.update(999L, req));
        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void update_partialFields_nullSkipped() {
        Streamer streamer = buildStreamer(1L, "原名称", Platform.DOUYIN, AccountType.OWN);
        when(streamerMapper.selectById(1L)).thenReturn(streamer);
        when(streamerMapper.updateById(any(Streamer.class))).thenReturn(1);

        UpdateStreamerRequest req = new UpdateStreamerRequest();
        req.setAnchorName("新名称");
        // platform and other fields are null, should not be modified

        StreamerVO result = streamerService.update(1L, req);
        assertEquals("新名称", result.getAnchorName());
        assertEquals("douyin", result.getPlatform()); // unchanged
    }

    @Test
    void delete_notFound_throws() {
        when(streamerMapper.selectById(999L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> streamerService.delete(999L));
        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void startMonitor_notFound_throws() {
        when(streamerMapper.selectById(999L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> streamerService.startMonitor(999L));
        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
    }

    private Streamer buildStreamer(Long id, String anchorName, Platform platform, AccountType accountType) {
        Streamer s = new Streamer();
        s.setId(id);
        s.setUserId(1L);
        s.setOrgId(5L);
        s.setPlatform(platform);
        s.setAnchorName(anchorName);
        s.setAccountType(accountType);
        s.setIsMonitoring(false);
        s.setTotalSessions(0);
        s.setTodaySessions(0);
        s.setStatus(1);
        return s;
    }
}
