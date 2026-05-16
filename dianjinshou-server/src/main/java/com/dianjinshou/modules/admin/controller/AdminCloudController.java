package com.dianjinshou.modules.admin.controller;

import com.dianjinshou.common.response.ApiResponse;
import com.dianjinshou.common.response.PageResult;
import com.dianjinshou.modules.admin.service.AdminCloudService;
import com.dianjinshou.modules.admin.vo.CloudStatsVO;
import com.dianjinshou.modules.storage.vo.CloudFileVO;
import com.dianjinshou.modules.storage.vo.ShareLinkVO;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/cloud")
public class AdminCloudController {

    private final AdminCloudService adminCloudService;

    public AdminCloudController(AdminCloudService adminCloudService) {
        this.adminCloudService = adminCloudService;
    }

    @GetMapping("/stats")
    public ApiResponse<CloudStatsVO> stats() {
        return ApiResponse.success(adminCloudService.getStats());
    }

    @GetMapping("/files")
    public ApiResponse<PageResult<CloudFileVO>> listFiles(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long orgId,
            @RequestParam(required = false) String fileType,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.success(adminCloudService.listFiles(page, size, orgId, fileType, keyword));
    }

    @GetMapping("/shares")
    public ApiResponse<PageResult<ShareLinkVO>> listShares(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status) {
        return ApiResponse.success(adminCloudService.listShares(page, size, status));
    }

    @PutMapping("/shares/{id}/disable")
    public ApiResponse<Void> disableShare(@PathVariable Long id) {
        adminCloudService.disableShare(id);
        return ApiResponse.success(null);
    }
}
