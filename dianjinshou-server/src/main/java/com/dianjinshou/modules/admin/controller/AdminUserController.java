package com.dianjinshou.modules.admin.controller;

import com.dianjinshou.common.response.ApiResponse;
import com.dianjinshou.common.response.PageResult;
import com.dianjinshou.modules.admin.dto.AdminUserUpdateRequest;
import com.dianjinshou.modules.admin.service.AdminDataService;
import com.dianjinshou.modules.admin.service.AdminService;
import com.dianjinshou.modules.admin.vo.AdminUserDetailVO;
import com.dianjinshou.modules.admin.vo.AdminUserVO;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/users")
public class AdminUserController {

    private final AdminService adminService;
    private final AdminDataService adminDataService;

    public AdminUserController(AdminService adminService, AdminDataService adminDataService) {
        this.adminService = adminService;
        this.adminDataService = adminDataService;
    }

    @GetMapping("/{id}")
    public ApiResponse<AdminUserDetailVO> detail(@PathVariable Long id) {
        return ApiResponse.success(adminDataService.userDetail(id));
    }

    @GetMapping("/{id}/related-counts")
    public ApiResponse<Map<String, Long>> relatedCounts(@PathVariable Long id) {
        return ApiResponse.success(adminDataService.userRelatedCounts(id));
    }

    @GetMapping
    public ApiResponse<PageResult<AdminUserVO>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Integer status) {
        return ApiResponse.success(adminService.listUsers(page, size, keyword, role, status));
    }

    @PutMapping("/{id}")
    public ApiResponse<AdminUserVO> update(@PathVariable Long id,
                                           @RequestBody AdminUserUpdateRequest request) {
        return ApiResponse.success(adminService.updateUser(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        adminService.deleteUser(id);
        return ApiResponse.success();
    }
}
