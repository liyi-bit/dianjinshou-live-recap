package com.dianjinshou.modules.storage.controller;

import com.dianjinshou.common.response.ApiResponse;
import com.dianjinshou.modules.storage.dto.InitUploadRequest;
import com.dianjinshou.modules.storage.service.ChunkedUploadService;
import com.dianjinshou.modules.storage.vo.UploadInitVO;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/upload")
public class UploadController {

    private final ChunkedUploadService chunkedUploadService;

    public UploadController(ChunkedUploadService chunkedUploadService) {
        this.chunkedUploadService = chunkedUploadService;
    }

    @PostMapping("/init")
    public ApiResponse<UploadInitVO> initUpload(@Valid @RequestBody InitUploadRequest request) {
        return ApiResponse.success(chunkedUploadService.initUpload(request));
    }

    @PutMapping("/{uploadId}/part/{partNumber}")
    public ApiResponse<Void> uploadPart(@PathVariable Long uploadId,
                                         @PathVariable int partNumber,
                                         @RequestParam("file") MultipartFile file) throws IOException {
        chunkedUploadService.uploadPart(uploadId, partNumber, file.getInputStream(), file.getSize());
        return ApiResponse.success();
    }

    @PostMapping("/{uploadId}/complete")
    public ApiResponse<Map<String, String>> completeUpload(@PathVariable Long uploadId) {
        String storageKey = chunkedUploadService.completeUpload(uploadId);
        return ApiResponse.success(Collections.singletonMap("storageKey", storageKey));
    }

    @DeleteMapping("/{uploadId}")
    public ApiResponse<Void> cancelUpload(@PathVariable Long uploadId) {
        chunkedUploadService.cancelUpload(uploadId);
        return ApiResponse.success();
    }
}
