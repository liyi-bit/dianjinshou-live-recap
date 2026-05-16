package com.dianjinshou.modules.recap.controller;

import com.dianjinshou.common.response.ApiResponse;
import com.dianjinshou.common.security.RateLimit;
import com.dianjinshou.modules.recap.service.DiagnosisService;
import com.dianjinshou.modules.recap.vo.DiagnosisReportVO;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/analysis")
public class DiagnosisController {

    private final DiagnosisService diagnosisService;

    public DiagnosisController(DiagnosisService diagnosisService) {
        this.diagnosisService = diagnosisService;
    }

    @PostMapping("/{id}/diagnosis")
    @RateLimit(max = 5, windowSeconds = 60, key = "ai:diagnosis")
    public ApiResponse<DiagnosisReportVO> generateDiagnosis(@PathVariable Long id) {
        return ApiResponse.success(diagnosisService.generateDiagnosis(id));
    }

    @GetMapping("/{id}/diagnosis-report")
    public ApiResponse<DiagnosisReportVO> getDiagnosisReport(@PathVariable Long id) {
        return ApiResponse.success(diagnosisService.getDiagnosis(id));
    }
}
