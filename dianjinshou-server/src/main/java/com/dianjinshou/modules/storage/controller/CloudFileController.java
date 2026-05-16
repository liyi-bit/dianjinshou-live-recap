package com.dianjinshou.modules.storage.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dianjinshou.common.response.ApiResponse;
import com.dianjinshou.modules.storage.service.CloudFileService;
import com.dianjinshou.modules.storage.vo.CloudFileVO;
import com.dianjinshou.modules.storage.vo.CloudUsageVO;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/cloud/files")
public class CloudFileController {

    private final CloudFileService cloudFileService;

    public CloudFileController(CloudFileService cloudFileService) {
        this.cloudFileService = cloudFileService;
    }

    @GetMapping
    public ApiResponse<Page<CloudFileVO>> listFiles(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String fileType,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.success(cloudFileService.listFiles(page, size, fileType, keyword));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteFile(@PathVariable Long id) {
        cloudFileService.deleteFile(id);
        return ApiResponse.success();
    }

    @GetMapping("/{id}/download")
    public ApiResponse<Map<String, String>> getDownloadUrl(@PathVariable Long id) {
        String url = cloudFileService.getDownloadUrl(id);
        return ApiResponse.success(Collections.singletonMap("downloadUrl", url));
    }

    @GetMapping("/usage")
    public ApiResponse<CloudUsageVO> getUsage(@RequestParam(defaultValue = "0") int vipLevel) {
        return ApiResponse.success(cloudFileService.getUsage(vipLevel));
    }
}
