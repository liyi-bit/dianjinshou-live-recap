package com.dianjinshou.modules.admin.controller;

import com.dianjinshou.common.response.ApiResponse;
import com.dianjinshou.modules.admin.dto.CreateOrgRequest;
import com.dianjinshou.modules.admin.service.AdminService;
import com.dianjinshou.modules.admin.vo.AdminOrgVO;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/organizations")
public class AdminOrgController {

    private final AdminService adminService;

    public AdminOrgController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping
    public ApiResponse<List<AdminOrgVO>> list() {
        return ApiResponse.success(adminService.listOrganizations());
    }

    @PostMapping
    public ApiResponse<AdminOrgVO> create(@Valid @RequestBody CreateOrgRequest request) {
        return ApiResponse.success(adminService.createOrganization(request));
    }
}
