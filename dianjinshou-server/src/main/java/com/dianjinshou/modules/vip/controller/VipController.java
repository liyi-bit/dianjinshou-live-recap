package com.dianjinshou.modules.vip.controller;

import com.dianjinshou.common.response.ApiResponse;
import com.dianjinshou.modules.vip.entity.VipPlan;
import com.dianjinshou.modules.vip.service.VipService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/vip")
public class VipController {

    private final VipService vipService;

    public VipController(VipService vipService) {
        this.vipService = vipService;
    }

    @GetMapping("/plans")
    public ApiResponse<List<VipPlan>> listPlans() {
        return ApiResponse.success(vipService.listPlans());
    }

    @GetMapping("/quota")
    public ApiResponse<Map<String, Object>> getQuota() {
        return ApiResponse.success(vipService.getQuotaInfo());
    }

    @PostMapping("/redeem")
    public ApiResponse<Map<String, String>> redeem(@RequestBody Map<String, String> body) {
        String result = vipService.redeemCode(body.get("code"));
        Map<String, String> resp = new java.util.HashMap<>();
        resp.put("message", result);
        return ApiResponse.success(resp);
    }
}
