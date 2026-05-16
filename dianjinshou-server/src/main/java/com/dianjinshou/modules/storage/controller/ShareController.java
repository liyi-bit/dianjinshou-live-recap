package com.dianjinshou.modules.storage.controller;

import com.dianjinshou.common.response.ApiResponse;
import com.dianjinshou.common.security.RateLimit;
import com.dianjinshou.modules.storage.dto.CreateShareRequest;
import com.dianjinshou.modules.storage.service.ShareLinkService;
import com.dianjinshou.modules.storage.vo.ShareLinkVO;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class ShareController {

    private final ShareLinkService shareLinkService;

    public ShareController(ShareLinkService shareLinkService) {
        this.shareLinkService = shareLinkService;
    }

    @PostMapping("/api/v1/cloud/files/{id}/share")
    public ApiResponse<ShareLinkVO> createShare(@PathVariable Long id,
                                                 @RequestBody CreateShareRequest request) {
        return ApiResponse.success(shareLinkService.createShare(id, request));
    }

    @GetMapping("/api/v1/cloud/shares")
    public ApiResponse<List<ShareLinkVO>> myShares() {
        return ApiResponse.success(shareLinkService.listMyShares());
    }

    @DeleteMapping("/api/v1/cloud/shares/{id}")
    public ApiResponse<Void> cancelShare(@PathVariable Long id) {
        shareLinkService.cancelShare(id);
        return ApiResponse.success();
    }

    @GetMapping("/api/v1/share/{shareCode}")
    @RateLimit(max = 30, windowSeconds = 60, key = "share:access")
    public ApiResponse<ShareLinkService.ShareAccessResult> accessShare(
            @PathVariable String shareCode,
            @RequestParam(required = false) String password) {
        return ApiResponse.success(shareLinkService.accessShare(shareCode, password));
    }
}
