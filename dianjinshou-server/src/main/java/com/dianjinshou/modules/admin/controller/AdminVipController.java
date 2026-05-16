package com.dianjinshou.modules.admin.controller;

import com.dianjinshou.common.response.ApiResponse;
import com.dianjinshou.modules.admin.dto.VipPlanRequest;
import com.dianjinshou.modules.admin.service.AdminService;
import com.dianjinshou.modules.vip.entity.VipPlan;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/vip-plans")
public class AdminVipController {

    private final AdminService adminService;

    public AdminVipController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping
    public ApiResponse<List<VipPlan>> list() {
        return ApiResponse.success(adminService.listVipPlans());
    }

    @PostMapping
    public ApiResponse<VipPlan> create(@Valid @RequestBody VipPlanRequest request) {
        return ApiResponse.success(adminService.createVipPlan(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<VipPlan> update(@PathVariable Long id,
                                       @Valid @RequestBody VipPlanRequest request) {
        return ApiResponse.success(adminService.updateVipPlan(id, request));
    }

    @PutMapping("/{id}/toggle")
    public ApiResponse<Void> toggle(@PathVariable Long id) {
        adminService.toggleVipPlan(id);
        return ApiResponse.success();
    }
}
