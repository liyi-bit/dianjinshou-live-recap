package com.dianjinshou.modules.shortclip.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dianjinshou.common.response.ApiResponse;
import com.dianjinshou.common.security.RateLimit;
import com.dianjinshou.modules.shortclip.dto.CreateShortClipRequest;
import com.dianjinshou.modules.shortclip.service.ClipExportService;
import com.dianjinshou.modules.shortclip.service.ShortClipService;
import com.dianjinshou.modules.shortclip.vo.ShortClipVO;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/short-clips")
public class ShortClipController {

    private final ShortClipService shortClipService;
    private final ClipExportService clipExportService;

    public ShortClipController(ShortClipService shortClipService, ClipExportService clipExportService) {
        this.shortClipService = shortClipService;
        this.clipExportService = clipExportService;
    }

    @PostMapping
    @RateLimit(max = 10, windowSeconds = 60, key = "short:clip")
    public ApiResponse<ShortClipVO> createClip(@Valid @RequestBody CreateShortClipRequest request) {
        return ApiResponse.success(shortClipService.createClip(request));
    }

    @GetMapping
    public ApiResponse<Page<ShortClipVO>> listClips(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long recordingId,
            @RequestParam(required = false) String status) {
        return ApiResponse.success(shortClipService.listClips(page, size, recordingId, status));
    }

    @GetMapping("/{id}")
    public ApiResponse<ShortClipVO> getClip(@PathVariable Long id) {
        return ApiResponse.success(shortClipService.getClip(id));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteClip(@PathVariable Long id) {
        shortClipService.deleteClip(id);
        return ApiResponse.success();
    }

    @PostMapping("/batch-export")
    public ApiResponse<Map<String, String>> batchExport(@RequestBody List<Long> clipIds) {
        String exportKey = clipExportService.batchExport(clipIds);
        return ApiResponse.success(Collections.singletonMap("exportKey", exportKey));
    }

    @PostMapping("/{id}/upload-cloud")
    public ApiResponse<Void> uploadToCloud(@PathVariable Long id) {
        clipExportService.uploadToCloud(id);
        return ApiResponse.success();
    }
}
