package com.dianjinshou.modules.recording.controller;

import com.dianjinshou.common.response.ApiResponse;
import com.dianjinshou.common.response.PageResult;
import com.dianjinshou.modules.recording.dto.CompleteRecordingRequest;
import com.dianjinshou.modules.recording.dto.CreateRecordingRequest;
import com.dianjinshou.modules.recording.dto.RecordingQueryRequest;
import com.dianjinshou.modules.recording.dto.RenameRequest;
import com.dianjinshou.modules.recording.service.RecordingService;
import com.dianjinshou.modules.recording.vo.RecordingListVO;
import com.dianjinshou.modules.recording.vo.RecordingVO;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/recordings")
public class RecordingController {

    private final RecordingService recordingService;

    public RecordingController(RecordingService recordingService) {
        this.recordingService = recordingService;
    }

    @PostMapping
    public ApiResponse<RecordingVO> create(@Valid @RequestBody CreateRecordingRequest request) {
        RecordingVO vo = recordingService.create(request);
        return ApiResponse.success(vo);
    }

    @PostMapping("/{id}/complete")
    public ApiResponse<RecordingVO> complete(@PathVariable Long id,
                                             @Valid @RequestBody CompleteRecordingRequest request) {
        RecordingVO vo = recordingService.complete(id, request);
        return ApiResponse.success(vo);
    }

    @GetMapping
    public ApiResponse<PageResult<RecordingListVO>> list(RecordingQueryRequest request) {
        PageResult<RecordingListVO> result = recordingService.list(request);
        return ApiResponse.success(result);
    }

    /**
     * v1.1.0：桌面启动时用，拉当前用户卡在 transcribing 的录制列表，自行决定续跑 or 标 failed。
     */
    @GetMapping("/pending-asr")
    public ApiResponse<List<RecordingVO>> pendingAsr() {
        return ApiResponse.success(recordingService.listPendingAsr());
    }

    /**
     * v1.1.0：桌面端确认本地 MP4 已丢失、无法重跑 ASR 时把状态标记为 failed。
     */
    @PostMapping("/{id}/mark-asr-failed")
    public ApiResponse<Void> markAsrFailed(@PathVariable Long id,
                                           @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.getOrDefault("reason", "本地视频文件缺失，无法生成逐字稿") : null;
        recordingService.markAsrFailed(id, reason);
        return ApiResponse.success(null);
    }

    @GetMapping("/{id}")
    public ApiResponse<RecordingVO> detail(@PathVariable Long id) {
        RecordingVO vo = recordingService.detail(id);
        return ApiResponse.success(vo);
    }

    @PutMapping("/{id}/name")
    public ApiResponse<RecordingVO> rename(@PathVariable Long id,
                                           @Valid @RequestBody RenameRequest request) {
        RecordingVO vo = recordingService.rename(id, request);
        return ApiResponse.success(vo);
    }

    @PostMapping("/{id}/export")
    public ApiResponse<Map<String, Object>> export(@PathVariable Long id) {
        Map<String, Object> data = recordingService.export(id);
        return ApiResponse.success(data);
    }

    @DeleteMapping
    public ApiResponse<Map<String, Object>> batchDelete(@RequestBody Map<String, List<Long>> body) {
        List<Long> ids = body.get("ids");
        int count = recordingService.batchDelete(ids);
        Map<String, Object> data = new HashMap<>();
        data.put("deletedCount", count);
        return ApiResponse.success(data);
    }
}
