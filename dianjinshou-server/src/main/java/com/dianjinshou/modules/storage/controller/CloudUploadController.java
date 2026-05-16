package com.dianjinshou.modules.storage.controller;

import com.dianjinshou.common.response.ApiResponse;
import com.dianjinshou.modules.storage.dto.CloudUploadCompleteRequest;
import com.dianjinshou.modules.storage.dto.CloudUploadFailRequest;
import com.dianjinshou.modules.storage.dto.CloudUploadInitRequest;
import com.dianjinshou.modules.storage.dto.CloudUploadProgressRequest;
import com.dianjinshou.modules.storage.service.ChunkedUploadService;
import com.dianjinshou.modules.storage.vo.CloudUploadInitVO;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/v1/cloud-space/uploads")
public class CloudUploadController {

    private final ChunkedUploadService uploadService;

    public CloudUploadController(ChunkedUploadService uploadService) {
        this.uploadService = uploadService;
    }

    @PostMapping("/init")
    public ApiResponse<CloudUploadInitVO> init(@Valid @RequestBody CloudUploadInitRequest request) {
        return ApiResponse.success(uploadService.initCloudUpload(request));
    }

    @PostMapping("/{taskId}/progress")
    public ApiResponse<Void> progress(@PathVariable Long taskId,
                                      @Valid @RequestBody CloudUploadProgressRequest request) {
        uploadService.updateCloudProgress(taskId, request);
        return ApiResponse.success();
    }

    @PostMapping("/{taskId}/complete")
    public ApiResponse<Void> complete(@PathVariable Long taskId,
                                      @RequestBody(required = false) CloudUploadCompleteRequest request) {
        uploadService.completeCloudUpload(taskId, request);
        return ApiResponse.success();
    }

    @PostMapping("/{taskId}/fail")
    public ApiResponse<Void> fail(@PathVariable Long taskId,
                                  @RequestBody(required = false) CloudUploadFailRequest request) {
        uploadService.failCloudUpload(taskId, request);
        return ApiResponse.success();
    }
}
