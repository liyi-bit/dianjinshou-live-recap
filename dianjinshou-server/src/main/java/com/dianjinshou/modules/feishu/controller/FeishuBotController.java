package com.dianjinshou.modules.feishu.controller;

import com.dianjinshou.common.response.ApiResponse;
import com.dianjinshou.modules.feishu.dto.CreateFeishuBotRequest;
import com.dianjinshou.modules.feishu.entity.FeishuBot;
import com.dianjinshou.modules.feishu.service.FeishuBotService;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/feishu/bots")
public class FeishuBotController {

    private static final String SECRET_MASK = "********";

    private final FeishuBotService service;

    public FeishuBotController(FeishuBotService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> list() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (FeishuBot b : service.listMine()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", b.getId());
            m.put("appId", b.getAppId());
            m.put("botName", b.getBotName());
            m.put("status", b.getStatus());
            m.put("lastConnectedAt", b.getLastConnectedAt());
            m.put("lastError", b.getLastError());
            m.put("appSecret", SECRET_MASK);
            out.add(m);
        }
        return ApiResponse.success(out);
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> create(@Valid @RequestBody CreateFeishuBotRequest req) {
        FeishuBot b = service.create(req);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", b.getId());
        m.put("appId", b.getAppId());
        m.put("botName", b.getBotName());
        return ApiResponse.success(m);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ApiResponse.success();
    }
}
