package com.dianjinshou.modules.shortvideo.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dianjinshou.common.response.ApiResponse;
import com.dianjinshou.common.security.RateLimit;
import com.dianjinshou.modules.shortvideo.dto.ExtractCopywritingRequest;
import com.dianjinshou.modules.shortvideo.service.CopywritingExtractService;
import com.dianjinshou.modules.shortvideo.service.CreatorSearchService;
import com.dianjinshou.modules.shortvideo.vo.CreatorDetailVO;
import com.dianjinshou.modules.shortvideo.vo.CreatorVO;
import com.dianjinshou.modules.shortvideo.vo.VideoCopywritingVO;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/v1/short-video")
public class ShortVideoController {

    private final CopywritingExtractService copywritingExtractService;
    private final CreatorSearchService creatorSearchService;

    public ShortVideoController(CopywritingExtractService copywritingExtractService,
                                CreatorSearchService creatorSearchService) {
        this.copywritingExtractService = copywritingExtractService;
        this.creatorSearchService = creatorSearchService;
    }

    @PostMapping("/extract-copywriting")
    @RateLimit(max = 10, windowSeconds = 60, key = "copywriting:extract")
    public ApiResponse<VideoCopywritingVO> extractCopywriting(@Valid @RequestBody ExtractCopywritingRequest request) {
        return ApiResponse.success(copywritingExtractService.extractCopywriting(request));
    }

    @GetMapping("/copywriting")
    public ApiResponse<Page<VideoCopywritingVO>> listCopywriting(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status) {
        return ApiResponse.success(copywritingExtractService.listCopywriting(page, size, status));
    }

    @GetMapping("/copywriting/{id}")
    public ApiResponse<VideoCopywritingVO> getCopywriting(@PathVariable Long id) {
        return ApiResponse.success(copywritingExtractService.getCopywriting(id));
    }

    @PostMapping("/copywriting/{id}/copy")
    public ApiResponse<Void> recordCopy(@PathVariable Long id) {
        copywritingExtractService.recordCopy(id);
        return ApiResponse.success();
    }

    @DeleteMapping("/copywriting/{id}")
    public ApiResponse<Void> deleteCopywriting(@PathVariable Long id) {
        copywritingExtractService.deleteCopywriting(id);
        return ApiResponse.success();
    }

    // ========== Creator search ==========

    @GetMapping("/creators/search")
    public ApiResponse<List<CreatorVO>> searchCreators(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String platform,
            @RequestParam(required = false) String industry,
            @RequestParam(required = false) Long minFollowers,
            @RequestParam(required = false) Long maxFollowers,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(creatorSearchService.searchCreators(
                keyword, platform, industry, minFollowers, maxFollowers, page, size));
    }

    @GetMapping("/creators/{id}")
    public ApiResponse<CreatorDetailVO> getCreatorDetail(@PathVariable Long id) {
        return ApiResponse.success(creatorSearchService.getCreatorDetail(id));
    }
}
