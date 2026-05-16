package com.dianjinshou.modules.streamer.controller;

import com.dianjinshou.common.response.ApiResponse;
import com.dianjinshou.common.response.PageResult;
import com.dianjinshou.modules.streamer.dto.CreateStreamerRequest;
import com.dianjinshou.modules.streamer.dto.StreamerQueryRequest;
import com.dianjinshou.modules.streamer.dto.UpdateStreamerRequest;
import com.dianjinshou.modules.streamer.service.StreamerService;
import com.dianjinshou.modules.streamer.vo.StreamerListVO;
import com.dianjinshou.modules.streamer.vo.StreamerStatsVO;
import com.dianjinshou.modules.streamer.vo.StreamerVO;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/streamers")
public class StreamerController {

    private final StreamerService streamerService;

    public StreamerController(StreamerService streamerService) {
        this.streamerService = streamerService;
    }

    @PostMapping
    public ApiResponse<StreamerVO> create(@Valid @RequestBody CreateStreamerRequest request) {
        StreamerVO vo = streamerService.create(request);
        return ApiResponse.success(vo);
    }

    @GetMapping
    public ApiResponse<PageResult<StreamerListVO>> list(StreamerQueryRequest request) {
        PageResult<StreamerListVO> result = streamerService.list(request);
        return ApiResponse.success(result);
    }

    @GetMapping("/{id}")
    public ApiResponse<StreamerVO> detail(@PathVariable Long id) {
        StreamerVO vo = streamerService.detail(id);
        return ApiResponse.success(vo);
    }

    @PutMapping("/{id}")
    public ApiResponse<StreamerVO> update(@PathVariable Long id,
                                          @Valid @RequestBody UpdateStreamerRequest request) {
        StreamerVO vo = streamerService.update(id, request);
        return ApiResponse.success(vo);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Map<String, Object>> delete(@PathVariable Long id) {
        int deletedRecordings = streamerService.delete(id);
        Map<String, Object> data = new HashMap<>();
        data.put("deletedRecordings", deletedRecordings);
        return ApiResponse.success(data);
    }

    @PostMapping("/{id}/monitor/start")
    public ApiResponse<Void> startMonitor(@PathVariable Long id) {
        streamerService.startMonitor(id);
        return ApiResponse.success();
    }

    @PostMapping("/{id}/monitor/stop")
    public ApiResponse<Void> stopMonitor(@PathVariable Long id) {
        streamerService.stopMonitor(id);
        return ApiResponse.success();
    }

    @GetMapping("/stats")
    public ApiResponse<StreamerStatsVO> stats() {
        StreamerStatsVO vo = streamerService.stats();
        return ApiResponse.success(vo);
    }

    @PostMapping("/shipinhao/auth-url")
    public ApiResponse<Map<String, String>> shipinhaoAuthUrl() {
        Map<String, String> data = new HashMap<>();
        data.put("message", "TODO: 视频号授权URL生成待实现");
        return ApiResponse.success(data);
    }

    @PostMapping("/shipinhao/auth-callback")
    public ApiResponse<Map<String, String>> shipinhaoAuthCallback() {
        Map<String, String> data = new HashMap<>();
        data.put("message", "TODO: 视频号授权回调处理待实现");
        return ApiResponse.success(data);
    }
}
