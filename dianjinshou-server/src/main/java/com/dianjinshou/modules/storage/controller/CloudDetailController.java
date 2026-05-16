package com.dianjinshou.modules.storage.controller;

import com.dianjinshou.common.response.ApiResponse;
import com.dianjinshou.modules.storage.dto.CloudRestoreCompleteRequest;
import com.dianjinshou.modules.storage.service.CloudRestoreService;
import com.dianjinshou.modules.storage.vo.CloudFileVO;
import com.dianjinshou.modules.storage.vo.CloudReadonlyDetailVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/v1/cloud-space/files")
public class CloudDetailController {

    private final CloudRestoreService cloudRestoreService;

    public CloudDetailController(CloudRestoreService cloudRestoreService) {
        this.cloudRestoreService = cloudRestoreService;
    }

    @GetMapping("/{id}/readonly-detail")
    public ApiResponse<CloudReadonlyDetailVO> readonlyDetail(@PathVariable Long id) {
        return ApiResponse.success(cloudRestoreService.readonlyDetail(id));
    }

    @PostMapping("/{id}/download-to-local-complete")
    public ApiResponse<CloudFileVO> downloadToLocalComplete(@PathVariable Long id,
                                                            @Valid @RequestBody CloudRestoreCompleteRequest request) {
        return ApiResponse.success(cloudRestoreService.markDownloadToLocalComplete(id, request.getLocalFilePath()));
    }
}
