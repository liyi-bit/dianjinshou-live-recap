package com.dianjinshou.modules.comparison.controller;

import com.dianjinshou.common.response.ApiResponse;
import com.dianjinshou.modules.comparison.dto.CreateDraftRequest;
import com.dianjinshou.modules.comparison.dto.SelectSecondRequest;
import com.dianjinshou.modules.comparison.service.ComparisonDraftService;
import com.dianjinshou.modules.comparison.vo.ComparisonDraftVO;
import com.dianjinshou.modules.comparison.vo.ComparisonVO;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/v1/comparison-drafts")
public class ComparisonDraftController {

    private final ComparisonDraftService draftService;

    public ComparisonDraftController(ComparisonDraftService draftService) {
        this.draftService = draftService;
    }

    @PostMapping
    public ApiResponse<ComparisonDraftVO> createDraft(@Valid @RequestBody CreateDraftRequest request) {
        return ApiResponse.success(draftService.createDraft(request));
    }

    @GetMapping("/current")
    public ApiResponse<ComparisonDraftVO> getCurrent() {
        return ApiResponse.success(draftService.getCurrent());
    }

    @PostMapping("/{id}/select-second")
    public ApiResponse<ComparisonVO> selectSecond(@PathVariable Long id,
                                                   @Valid @RequestBody SelectSecondRequest request) {
        return ApiResponse.success(draftService.selectSecondAndConfirm(id, request));
    }

    @DeleteMapping("/current")
    public ApiResponse<Void> cancel() {
        draftService.cancelCurrent();
        return ApiResponse.success(null);
    }
}
