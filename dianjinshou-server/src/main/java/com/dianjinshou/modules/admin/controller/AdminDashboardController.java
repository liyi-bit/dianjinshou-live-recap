package com.dianjinshou.modules.admin.controller;

import com.dianjinshou.common.response.ApiResponse;
import com.dianjinshou.modules.admin.service.AdminService;
import com.dianjinshou.modules.admin.vo.DashboardStatsVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/dashboard")
public class AdminDashboardController {

    private final AdminService adminService;

    public AdminDashboardController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping
    public ApiResponse<DashboardStatsVO> getStats() {
        return ApiResponse.success(adminService.getDashboardStats());
    }
}
