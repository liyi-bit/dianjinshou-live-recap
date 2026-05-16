package com.dianjinshou.modules.recap.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dianjinshou.common.exception.BusinessException;
import com.dianjinshou.common.security.SecurityUser;
import com.dianjinshou.modules.recap.dto.CreateCompetitorReportRequest;
import com.dianjinshou.modules.recap.entity.CompetitorReport;
import com.dianjinshou.modules.recap.mapper.CompetitorReportMapper;
import com.dianjinshou.modules.recap.vo.CompetitorReportVO;
import com.dianjinshou.modules.streamer.entity.Streamer;
import com.dianjinshou.modules.streamer.mapper.StreamerMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompetitorAnalysisServiceTest {

    @Mock private CompetitorReportMapper competitorReportMapper;
    @Mock private StreamerMapper streamerMapper;

    private CompetitorAnalysisService service;

    @BeforeAll
    static void initTableInfo() {
        try {
            MapperBuilderAssistant a1 = new MapperBuilderAssistant(new MybatisConfiguration(), "");
            a1.setCurrentNamespace("com.dianjinshou.modules.recap.competitor.CompetitorReportMapper");
            TableInfoHelper.initTableInfo(a1, CompetitorReport.class);

            MapperBuilderAssistant a2 = new MapperBuilderAssistant(new MybatisConfiguration(), "");
            a2.setCurrentNamespace("com.dianjinshou.modules.recap.competitor.StreamerMapper");
            TableInfoHelper.initTableInfo(a2, Streamer.class);
        } catch (Exception ignored) { }
    }

    @BeforeEach
    void setUp() {
        service = new CompetitorAnalysisService(competitorReportMapper, streamerMapper);
        setCurrentUser(1L, "super_admin", 100L);
    }

    private void setCurrentUser(Long userId, String role, Long orgId) {
        SecurityUser user = new SecurityUser(userId, role, orgId);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private Streamer makeStreamer(Long id, Long industryId, String name) {
        Streamer s = new Streamer();
        s.setId(id);
        s.setIndustryId(industryId);
        s.setAnchorName(name);
        return s;
    }

    @Test
    void createReport_success() {
        Streamer s1 = makeStreamer(1L, 10L, "主播A");
        Streamer s2 = makeStreamer(2L, 10L, "主播B");

        when(streamerMapper.selectById(1L)).thenReturn(s1);
        when(streamerMapper.selectById(2L)).thenReturn(s2);
        when(competitorReportMapper.insert(any(CompetitorReport.class))).thenReturn(1);

        CreateCompetitorReportRequest req = new CreateCompetitorReportRequest();
        req.setStreamerId(1L);
        req.setCompetitorStreamerId(2L);

        CompetitorReportVO vo = service.createReport(req);

        assertNotNull(vo);
        assertEquals("主播A", vo.getStreamerName());
        assertEquals("主播B", vo.getCompetitorStreamerName());
        assertEquals("completed", vo.getStatus());
        verify(competitorReportMapper).insert(any(CompetitorReport.class));
    }

    @Test
    void createReport_differentIndustry_throws() {
        Streamer s1 = makeStreamer(1L, 10L, "主播A");
        Streamer s2 = makeStreamer(2L, 20L, "主播B");

        when(streamerMapper.selectById(1L)).thenReturn(s1);
        when(streamerMapper.selectById(2L)).thenReturn(s2);

        CreateCompetitorReportRequest req = new CreateCompetitorReportRequest();
        req.setStreamerId(1L);
        req.setCompetitorStreamerId(2L);

        assertThrows(BusinessException.class, () -> service.createReport(req));
    }

    @Test
    void createReport_streamerNotFound_throws() {
        when(streamerMapper.selectById(1L)).thenReturn(null);

        CreateCompetitorReportRequest req = new CreateCompetitorReportRequest();
        req.setStreamerId(1L);
        req.setCompetitorStreamerId(2L);

        assertThrows(BusinessException.class, () -> service.createReport(req));
    }

    @Test
    void getReport_notFound_throws() {
        when(competitorReportMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        assertThrows(BusinessException.class, () -> service.getReport(999L));
    }
}
