package com.dianjinshou.modules.comparison.controller;

import com.dianjinshou.common.response.ApiResponse;
import com.dianjinshou.common.response.PageResult;
import com.dianjinshou.modules.comparison.dto.CreateComparisonRequest;
import com.dianjinshou.modules.comparison.service.ComparisonService;
import com.dianjinshou.modules.comparison.vo.ComparisonVO;
import com.dianjinshou.modules.recap.entity.Keyword;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/v1/comparisons")
public class ComparisonController {

    private final ComparisonService comparisonService;

    public ComparisonController(ComparisonService comparisonService) {
        this.comparisonService = comparisonService;
    }

    @PostMapping
    public ApiResponse<ComparisonVO> create(@Valid @RequestBody CreateComparisonRequest request) {
        return ApiResponse.success(comparisonService.create(request));
    }

    @GetMapping
    public ApiResponse<PageResult<ComparisonVO>> list(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(comparisonService.list(type, status, startDate, endDate, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<ComparisonVO> detail(@PathVariable Long id) {
        return ApiResponse.success(comparisonService.detail(id));
    }

    @PostMapping("/{id}/swap")
    public ApiResponse<ComparisonVO> swap(@PathVariable Long id) {
        return ApiResponse.success(comparisonService.swap(id));
    }

    @DeleteMapping
    public ApiResponse<Void> batchDelete(@RequestBody List<Long> ids) {
        comparisonService.batchDelete(ids);
        return ApiResponse.success(null);
    }

    @GetMapping("/{id}/keywords")
    public ApiResponse<List<Keyword>> getKeywords(@PathVariable Long id) {
        return ApiResponse.success(comparisonService.getKeywords(id));
    }
}
