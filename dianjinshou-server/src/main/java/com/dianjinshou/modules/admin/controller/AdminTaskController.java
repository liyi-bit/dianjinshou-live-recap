package com.dianjinshou.modules.admin.controller;

import com.dianjinshou.common.response.ApiResponse;
import com.dianjinshou.common.response.PageResult;
import com.dianjinshou.modules.admin.service.AdminDataService;
import com.dianjinshou.modules.admin.vo.AdminTaskDetailVO;
import com.dianjinshou.modules.admin.vo.AdminTaskStatsVO;
import com.dianjinshou.modules.admin.vo.AdminTaskVO;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/admin/tasks")
public class AdminTaskController {

    private final AdminDataService adminDataService;

    public AdminTaskController(AdminDataService adminDataService) {
        this.adminDataService = adminDataService;
    }

    @GetMapping
    public ApiResponse<PageResult<AdminTaskVO>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String taskType,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String userPhone,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime start,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime end) {
        return ApiResponse.success(adminDataService.listTasks(page, size, taskType, userId, userPhone, status, start, end));
    }

    @GetMapping("/stats")
    public ApiResponse<AdminTaskStatsVO> stats() {
        return ApiResponse.success(adminDataService.taskStats());
    }

    @GetMapping("/{type}/{id}")
    public ApiResponse<AdminTaskDetailVO> detail(@PathVariable String type, @PathVariable Long id) {
        return ApiResponse.success(adminDataService.taskDetail(type, id));
    }
}
