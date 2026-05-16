package com.dianjinshou.modules.storage.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dianjinshou.common.response.ApiResponse;
import com.dianjinshou.modules.comparison.vo.ComparisonVO;
import com.dianjinshou.modules.storage.dto.CreateCloudComparisonRequest;
import com.dianjinshou.modules.storage.dto.RenameCloudFileRequest;
import com.dianjinshou.modules.storage.service.CloudFileService;
import com.dianjinshou.modules.storage.service.CloudSpaceService;
import com.dianjinshou.modules.storage.vo.CloudComparisonSourceStatusVO;
import com.dianjinshou.modules.storage.vo.CloudFileVO;
import com.dianjinshou.modules.storage.vo.CloudUploadStatusVO;
import com.dianjinshou.modules.storage.vo.CloudUsageVO;
import com.dianjinshou.modules.storage.vo.OpenTargetVO;
import com.dianjinshou.modules.storage.vo.SignedUrlVO;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/v1/cloud-space")
public class BusinessCloudSpaceController {

    private final CloudSpaceService cloudSpaceService;
    private final CloudFileService cloudFileService;

    public BusinessCloudSpaceController(CloudSpaceService cloudSpaceService,
                                        CloudFileService cloudFileService) {
        this.cloudSpaceService = cloudSpaceService;
        this.cloudFileService = cloudFileService;
    }

    @GetMapping("/full-recaps")
    public ApiResponse<Page<CloudFileVO>> fullRecaps(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long industryId,
            @RequestParam(required = false) String anchorName,
            @RequestParam(required = false) String uploadAccount,
            @RequestParam(required = false) String accountType,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        return ApiResponse.success(cloudSpaceService.listFullRecaps(page, size, keyword, industryId, anchorName, uploadAccount, accountType, startTime, endTime));
    }

    @GetMapping("/clip-recaps")
    public ApiResponse<Page<CloudFileVO>> clipRecaps(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long industryId,
            @RequestParam(required = false) String anchorName,
            @RequestParam(required = false) String uploadAccount,
            @RequestParam(required = false) String accountType,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        return ApiResponse.success(cloudSpaceService.listClipRecaps(page, size, keyword, industryId, anchorName, uploadAccount, accountType, startTime, endTime));
    }

    @GetMapping("/comparisons")
    public ApiResponse<Page<CloudFileVO>> comparisons(
            @RequestParam(defaultValue = "full") String mode,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String uploadAccount,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        return ApiResponse.success(cloudSpaceService.listComparisons(mode, page, size, keyword, uploadAccount, startTime, endTime));
    }

    @GetMapping("/comparison-candidates")
    public ApiResponse<Page<CloudFileVO>> comparisonCandidates(
            @RequestParam(defaultValue = "full") String mode,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long industryId,
            @RequestParam(required = false) String anchorName,
            @RequestParam(required = false) String uploadAccount,
            @RequestParam(required = false) String accountType,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        return ApiResponse.success(cloudSpaceService.listComparisonCandidates(mode, page, size, keyword, industryId, anchorName, uploadAccount, accountType, startTime, endTime));
    }

    @PostMapping("/comparisons")
    public ApiResponse<ComparisonVO> createComparison(@Valid @RequestBody CreateCloudComparisonRequest request) {
        return ApiResponse.success(cloudSpaceService.createComparison(request));
    }

    @GetMapping("/comparisons/{comparisonId}/source-status")
    public ApiResponse<CloudComparisonSourceStatusVO> comparisonSourceStatus(@PathVariable Long comparisonId) {
        return ApiResponse.success(cloudSpaceService.comparisonSourceStatus(comparisonId));
    }

    @GetMapping("/upload-status")
    public ApiResponse<CloudUploadStatusVO> uploadStatus(
            @RequestParam String businessType,
            @RequestParam(required = false) Long businessId,
            @RequestParam(required = false) Long recordingId,
            @RequestParam(required = false) Long clipId,
            @RequestParam(required = false) Long comparisonId) {
        return ApiResponse.success(cloudSpaceService.uploadStatus(businessType, businessId, recordingId, clipId, comparisonId));
    }

    @GetMapping("/usage")
    public ApiResponse<CloudUsageVO> usage() {
        return ApiResponse.success(cloudFileService.getUsage());
    }

    @PatchMapping("/files/{id}/display-name")
    public ApiResponse<CloudFileVO> rename(@PathVariable Long id,
                                           @Valid @RequestBody RenameCloudFileRequest request) {
        return ApiResponse.success(cloudFileService.rename(id, request));
    }

    @DeleteMapping("/files/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        cloudFileService.deleteFile(id);
        return ApiResponse.success();
    }

    @PostMapping("/files/{id}/signed-url")
    public ApiResponse<SignedUrlVO> signedUrl(@PathVariable Long id) {
        return ApiResponse.success(cloudFileService.signedUrl(id));
    }

    @PostMapping("/files/{id}/download-to-local-request")
    public ApiResponse<SignedUrlVO> downloadToLocal(@PathVariable Long id) {
        return ApiResponse.success(cloudFileService.signedUrl(id));
    }

    @GetMapping("/files/{id}/open-target")
    public ApiResponse<OpenTargetVO> openTarget(@PathVariable Long id) {
        return ApiResponse.success(cloudFileService.openTarget(id));
    }
}
