package com.dianjinshou.modules.fileanalysis.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dianjinshou.common.response.ApiResponse;
import com.dianjinshou.common.security.RateLimit;
import com.dianjinshou.modules.fileanalysis.dto.CreateFileAnalysisRequest;
import com.dianjinshou.modules.fileanalysis.service.FileAnalysisService;
import com.dianjinshou.modules.fileanalysis.vo.FileAnalysisVO;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/v1/file-analysis")
public class FileAnalysisController {

    private final FileAnalysisService fileAnalysisService;

    public FileAnalysisController(FileAnalysisService fileAnalysisService) {
        this.fileAnalysisService = fileAnalysisService;
    }

    @PostMapping
    @RateLimit(max = 5, windowSeconds = 60, key = "file:analysis")
    public ApiResponse<FileAnalysisVO> createAnalysis(@Valid @RequestBody CreateFileAnalysisRequest request) {
        return ApiResponse.success(fileAnalysisService.createAnalysis(request));
    }

    @GetMapping
    public ApiResponse<Page<FileAnalysisVO>> listAnalyses(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.success(fileAnalysisService.listAnalyses(page, size, status, keyword));
    }

    @GetMapping("/{id}")
    public ApiResponse<FileAnalysisVO> getAnalysis(@PathVariable Long id) {
        return ApiResponse.success(fileAnalysisService.getAnalysis(id));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteAnalysis(@PathVariable Long id) {
        fileAnalysisService.deleteAnalysis(id);
        return ApiResponse.success();
    }
}
