package com.dianjinshou.modules.admin.controller;

import com.dianjinshou.common.response.ApiResponse;
import com.dianjinshou.common.response.PageResult;
import com.dianjinshou.modules.admin.service.AdminService;
import com.dianjinshou.modules.admin.vo.OperationLogVO;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/logs")
public class AdminLogController {

    private final AdminService adminService;

    public AdminLogController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping
    public ApiResponse<PageResult<OperationLogVO>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String action) {
        return ApiResponse.success(adminService.listLogs(page, size, action));
    }
}
