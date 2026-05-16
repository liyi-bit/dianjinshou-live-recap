package com.dianjinshou.modules.shortvideo.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dianjinshou.common.response.ApiResponse;
import com.dianjinshou.modules.shortvideo.dto.SubscribeCreatorRequest;
import com.dianjinshou.modules.shortvideo.dto.SubscribeTrendingRequest;
import com.dianjinshou.modules.shortvideo.entity.CreatorSubscription;
import com.dianjinshou.modules.shortvideo.entity.TrendingAlert;
import com.dianjinshou.modules.shortvideo.entity.TrendingSubscription;
import com.dianjinshou.modules.shortvideo.service.SubscriptionService;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/short-video")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    public SubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @PostMapping("/subscriptions/creator")
    public ApiResponse<CreatorSubscription> subscribeCreator(@Valid @RequestBody SubscribeCreatorRequest request) {
        return ApiResponse.success(subscriptionService.subscribeCreator(request));
    }

    @PostMapping("/subscriptions/trending")
    public ApiResponse<TrendingSubscription> subscribeTrending(@RequestBody SubscribeTrendingRequest request) {
        return ApiResponse.success(subscriptionService.subscribeTrending(request));
    }

    @GetMapping("/subscriptions")
    public ApiResponse<Map<String, Object>> listSubscriptions() {
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("creators", subscriptionService.listCreatorSubscriptions());
        result.put("trending", subscriptionService.listTrendingSubscriptions());
        return ApiResponse.success(result);
    }

    @DeleteMapping("/subscriptions/{id}")
    public ApiResponse<Void> cancelSubscription(@PathVariable Long id, @RequestParam String type) {
        subscriptionService.cancelSubscription(id, type);
        return ApiResponse.success();
    }

    @GetMapping("/alerts")
    public ApiResponse<Page<TrendingAlert>> listAlerts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(subscriptionService.listAlerts(page, size));
    }

    @PutMapping("/alerts/{id}/read")
    public ApiResponse<Void> markAlertRead(@PathVariable Long id) {
        subscriptionService.markAlertRead(id);
        return ApiResponse.success();
    }
}
