package com.dianjinshou.modules.storage.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dianjinshou.common.response.ApiResponse;
import com.dianjinshou.modules.storage.service.CloudFileService;
import com.dianjinshou.modules.storage.service.CloudSpaceService;
import com.dianjinshou.modules.storage.vo.CloudFileVO;
import com.dianjinshou.modules.storage.vo.CloudUsageVO;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cloud")
public class CloudSpaceController {

    private final CloudSpaceService cloudSpaceService;
    private final CloudFileService cloudFileService;

    public CloudSpaceController(CloudSpaceService cloudSpaceService,
                                 CloudFileService cloudFileService) {
        this.cloudSpaceService = cloudSpaceService;
        this.cloudFileService = cloudFileService;
    }

    @GetMapping("/recordings")
    public ApiResponse<Page<CloudFileVO>> recordings(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String sortBy) {
        return ApiResponse.success(cloudSpaceService.listByType("recording", page, size, keyword, sortBy));
    }

    @GetMapping("/clips")
    public ApiResponse<Page<CloudFileVO>> clips(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String sortBy) {
        return ApiResponse.success(cloudSpaceService.listByType("clip", page, size, keyword, sortBy));
    }

    @GetMapping("/documents")
    public ApiResponse<Page<CloudFileVO>> documents(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String sortBy) {
        return ApiResponse.success(cloudSpaceService.listByType("document", page, size, keyword, sortBy));
    }

    @PostMapping("/batch-delete")
    public ApiResponse<Void> batchDelete(@RequestBody List<Long> ids) {
        cloudSpaceService.batchDelete(ids);
        return ApiResponse.success();
    }

    @PostMapping("/batch-download")
    public ApiResponse<List<String>> batchDownload(@RequestBody List<Long> ids) {
        return ApiResponse.success(cloudSpaceService.batchDownloadUrls(ids));
    }

    @GetMapping("/usage")
    public ApiResponse<CloudUsageVO> usage(@RequestParam(defaultValue = "0") int vipLevel) {
        return ApiResponse.success(cloudFileService.getUsage(vipLevel));
    }
}
