package com.dianjinshou.modules.recap.controller;

import com.dianjinshou.common.response.ApiResponse;
import com.dianjinshou.common.response.PageResult;
import com.dianjinshou.common.security.RateLimit;
import com.dianjinshou.modules.recap.dto.CreateCompetitorReportRequest;
import com.dianjinshou.modules.recap.service.CompetitorAnalysisService;
import com.dianjinshou.modules.recap.vo.CompetitorReportVO;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/v1/analysis")
public class CompetitorController {

    private final CompetitorAnalysisService competitorAnalysisService;

    public CompetitorController(CompetitorAnalysisService competitorAnalysisService) {
        this.competitorAnalysisService = competitorAnalysisService;
    }

    @PostMapping("/competitor-report")
    @RateLimit(max = 3, windowSeconds = 60, key = "ai:competitor")
    public ApiResponse<CompetitorReportVO> createReport(@Valid @RequestBody CreateCompetitorReportRequest request) {
        return ApiResponse.success(competitorAnalysisService.createReport(request));
    }

    @GetMapping("/competitor-reports")
    public ApiResponse<PageResult<CompetitorReportVO>> listReports(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(competitorAnalysisService.listReports(page, size));
    }

    @GetMapping("/competitor-reports/{id}")
    public ApiResponse<CompetitorReportVO> getReport(@PathVariable Long id) {
        return ApiResponse.success(competitorAnalysisService.getReport(id));
    }
}
