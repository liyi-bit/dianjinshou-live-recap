package com.dianjinshou.modules.recap.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dianjinshou.common.exception.BusinessException;
import com.dianjinshou.common.response.ErrorCode;
import com.dianjinshou.common.response.PageResult;
import com.dianjinshou.common.security.OrgScopeHelper;
import com.dianjinshou.common.security.SecurityContextHelper;
import com.dianjinshou.modules.recap.dto.CreateCompetitorReportRequest;
import com.dianjinshou.modules.recap.entity.CompetitorReport;
import com.dianjinshou.modules.recap.mapper.CompetitorReportMapper;
import com.dianjinshou.modules.recap.vo.CompetitorReportVO;
import com.dianjinshou.modules.streamer.entity.Streamer;
import com.dianjinshou.modules.streamer.mapper.StreamerMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CompetitorAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(CompetitorAnalysisService.class);

    private final CompetitorReportMapper competitorReportMapper;
    private final StreamerMapper streamerMapper;

    public CompetitorAnalysisService(CompetitorReportMapper competitorReportMapper,
                                     StreamerMapper streamerMapper) {
        this.competitorReportMapper = competitorReportMapper;
        this.streamerMapper = streamerMapper;
    }

    public CompetitorReportVO createReport(CreateCompetitorReportRequest request) {
        Long userId = SecurityContextHelper.currentUserId();
        Long orgId = SecurityContextHelper.currentOrgId();
        if (userId == null || orgId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        Streamer streamer = streamerMapper.selectById(request.getStreamerId());
        Streamer competitor = streamerMapper.selectById(request.getCompetitorStreamerId());

        if (streamer == null || competitor == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "主播不存在");
        }

        // Validate same industry
        if (streamer.getIndustryId() == null || !streamer.getIndustryId().equals(competitor.getIndustryId())) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "仅允许同行业主播对比");
        }

        CompetitorReport report = new CompetitorReport();
        report.setUserId(userId);
        report.setOrgId(orgId);
        report.setStreamerId(request.getStreamerId());
        report.setCompetitorStreamerId(request.getCompetitorStreamerId());
        report.setRecordingId(request.getRecordingId());
        report.setCompetitorRecordingId(request.getCompetitorRecordingId());
        report.setAiModel("doubao");
        report.setStatus("completed"); // Placeholder — in production this is async

        // Generate placeholder report JSON
        report.setReport(generatePlaceholderReport(streamer, competitor));

        competitorReportMapper.insert(report);
        log.info("Competitor report created: {} vs {}", request.getStreamerId(), request.getCompetitorStreamerId());

        return CompetitorReportVO.fromEntity(report, streamer.getAnchorName(), competitor.getAnchorName(),
                streamer.getAnchorAvatar(), competitor.getAnchorAvatar());
    }

    public PageResult<CompetitorReportVO> listReports(int page, int size) {
        LambdaQueryWrapper<CompetitorReport> query = new LambdaQueryWrapper<>();
        OrgScopeHelper.applyOrgScope(query, CompetitorReport::getOrgId);
        query.orderByDesc(CompetitorReport::getCreatedAt);

        Page<CompetitorReport> entityPage = competitorReportMapper.selectPage(new Page<>(page, size), query);
        List<CompetitorReportVO> items = new ArrayList<CompetitorReportVO>();
        for (CompetitorReport r : entityPage.getRecords()) {
            Streamer s = streamerMapper.selectById(r.getStreamerId());
            Streamer c = streamerMapper.selectById(r.getCompetitorStreamerId());
            items.add(CompetitorReportVO.fromEntity(r,
                    s != null ? s.getAnchorName() : "已删除",
                    c != null ? c.getAnchorName() : "已删除",
                    s != null ? s.getAnchorAvatar() : null,
                    c != null ? c.getAnchorAvatar() : null));
        }
        return PageResult.of(items, entityPage.getTotal(), page, size);
    }

    public CompetitorReportVO getReport(Long id) {
        LambdaQueryWrapper<CompetitorReport> query = new LambdaQueryWrapper<>();
        query.eq(CompetitorReport::getId, id);
        OrgScopeHelper.applyOrgScope(query, CompetitorReport::getOrgId);
        CompetitorReport report = competitorReportMapper.selectOne(query);

        if (report == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "竞品分析报告不存在");
        }

        Streamer s = streamerMapper.selectById(report.getStreamerId());
        Streamer c = streamerMapper.selectById(report.getCompetitorStreamerId());
        return CompetitorReportVO.fromEntity(report,
                s != null ? s.getAnchorName() : "已删除",
                c != null ? c.getAnchorName() : "已删除",
                s != null ? s.getAnchorAvatar() : null,
                c != null ? c.getAnchorAvatar() : null);
    }

    private String generatePlaceholderReport(Streamer streamer, Streamer competitor) {
        // In production this is generated by AI model
        return "{\"dimensions\":[" +
                "{\"name\":\"开场话术\",\"myScore\":75,\"competitorScore\":82}," +
                "{\"name\":\"产品介绍\",\"myScore\":80,\"competitorScore\":78}," +
                "{\"name\":\"互动引导\",\"myScore\":65,\"competitorScore\":88}," +
                "{\"name\":\"促单技巧\",\"myScore\":72,\"competitorScore\":70}," +
                "{\"name\":\"节奏把控\",\"myScore\":85,\"competitorScore\":79}," +
                "{\"name\":\"情绪感染力\",\"myScore\":70,\"competitorScore\":85}," +
                "{\"name\":\"专业知识\",\"myScore\":82,\"competitorScore\":76}," +
                "{\"name\":\"数据表现\",\"myScore\":68,\"competitorScore\":90}" +
                "],\"highlights\":[\"竞品在互动引导方面表现突出\",\"竞品的情绪感染力更强\"]," +
                "\"improvements\":[\"加强互动引导频次\",\"提升直播间情绪氛围\"]," +
                "\"summary\":\"双方各有优势，建议重点学习竞品的互动引导策略\"}";
    }
}
