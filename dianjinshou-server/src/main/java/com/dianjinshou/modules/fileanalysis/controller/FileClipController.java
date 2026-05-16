package com.dianjinshou.modules.fileanalysis.controller;

import com.dianjinshou.common.response.ApiResponse;
import com.dianjinshou.modules.fileanalysis.dto.CreateFileClipRequest;
import com.dianjinshou.modules.fileanalysis.service.FileClipService;
import com.dianjinshou.modules.fileanalysis.vo.FileAnalysisVO;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/v1/file-analysis/{fileAnalysisId}/clips")
public class FileClipController {

    private final FileClipService fileClipService;

    public FileClipController(FileClipService fileClipService) {
        this.fileClipService = fileClipService;
    }

    @PostMapping
    public ApiResponse<FileAnalysisVO> createClip(@PathVariable Long fileAnalysisId,
                                                    @Valid @RequestBody CreateFileClipRequest request) {
        return ApiResponse.success(fileClipService.createClip(fileAnalysisId, request));
    }

    @GetMapping
    public ApiResponse<List<FileAnalysisVO>> listClips(@PathVariable Long fileAnalysisId) {
        return ApiResponse.success(fileClipService.listClips(fileAnalysisId));
    }
}
