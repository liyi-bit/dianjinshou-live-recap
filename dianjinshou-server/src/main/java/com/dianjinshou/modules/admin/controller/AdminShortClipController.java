package com.dianjinshou.modules.admin.controller;

import com.dianjinshou.common.response.ApiResponse;
import com.dianjinshou.common.response.PageResult;
import com.dianjinshou.modules.admin.service.AdminShortClipService;
import com.dianjinshou.modules.admin.vo.ShortClipStatsVO;
import com.dianjinshou.modules.shortclip.vo.ShortClipVO;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/short-clips")
public class AdminShortClipController {

    private final AdminShortClipService adminShortClipService;

    public AdminShortClipController(AdminShortClipService adminShortClipService) {
        this.adminShortClipService = adminShortClipService;
    }

    @GetMapping
    public ApiResponse<PageResult<ShortClipVO>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long orgId,
            @RequestParam(required = false) String status) {
        return ApiResponse.success(adminShortClipService.listAll(page, size, orgId, status));
    }

    @GetMapping("/stats")
    public ApiResponse<ShortClipStatsVO> stats() {
        return ApiResponse.success(adminShortClipService.getStats());
    }
}
