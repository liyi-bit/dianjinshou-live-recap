package com.dianjinshou.modules.fileanalysis.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dianjinshou.common.response.ApiResponse;
import com.dianjinshou.common.security.RateLimit;
import com.dianjinshou.modules.fileanalysis.dto.CopywritingReviewRequest;
import com.dianjinshou.modules.fileanalysis.service.CopywritingReviewService;
import com.dianjinshou.modules.fileanalysis.vo.CopywritingReviewVO;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/v1/file-analysis/copywriting-review")
public class CopywritingReviewController {

    private final CopywritingReviewService copywritingReviewService;

    public CopywritingReviewController(CopywritingReviewService copywritingReviewService) {
        this.copywritingReviewService = copywritingReviewService;
    }

    @PostMapping
    @RateLimit(max = 10, windowSeconds = 60, key = "copywriting:review")
    public ApiResponse<CopywritingReviewVO> submitReview(@Valid @RequestBody CopywritingReviewRequest request) {
        return ApiResponse.success(copywritingReviewService.submitReview(request));
    }

    @GetMapping("s")
    public ApiResponse<Page<CopywritingReviewVO>> listReviews(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(copywritingReviewService.listReviews(page, size));
    }
}
