package com.dianjinshou.modules.admin.controller;

import com.dianjinshou.common.response.ApiResponse;
import com.dianjinshou.common.response.PageResult;
import com.dianjinshou.modules.admin.service.AdminDataService;
import com.dianjinshou.modules.admin.vo.AdminRecordingDetailVO;
import com.dianjinshou.modules.admin.vo.AdminRecordingStatsVO;
import com.dianjinshou.modules.admin.vo.AdminRecordingVO;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/admin/recordings")
public class AdminRecordingController {

    private final AdminDataService adminDataService;

    public AdminRecordingController(AdminDataService adminDataService) {
        this.adminDataService = adminDataService;
    }

    @GetMapping
    public ApiResponse<PageResult<AdminRecordingVO>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String userPhone,
            @RequestParam(required = false) String analysisStatus,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime start,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime end) {
        return ApiResponse.success(adminDataService.listRecordings(page, size, userId, userPhone, analysisStatus, start, end));
    }

    @GetMapping("/stats")
    public ApiResponse<AdminRecordingStatsVO> stats() {
        return ApiResponse.success(adminDataService.recordingStats());
    }

    @GetMapping("/{id}")
    public ApiResponse<AdminRecordingDetailVO> detail(@PathVariable Long id) {
        return ApiResponse.success(adminDataService.recordingDetail(id));
    }
}
