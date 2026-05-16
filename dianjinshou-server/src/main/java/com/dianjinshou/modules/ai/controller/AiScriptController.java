package com.dianjinshou.modules.ai.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dianjinshou.common.response.ApiResponse;
import com.dianjinshou.common.security.RateLimit;
import com.dianjinshou.modules.ai.dto.GenerateScriptRequest;
import com.dianjinshou.modules.ai.entity.ScriptGeneration;
import com.dianjinshou.modules.ai.service.ScriptAssistantService;
import com.dianjinshou.modules.ai.vo.ScriptTemplateVO;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/v1/ai/script")
public class AiScriptController {

    private final ScriptAssistantService scriptAssistantService;

    public AiScriptController(ScriptAssistantService scriptAssistantService) {
        this.scriptAssistantService = scriptAssistantService;
    }

    @GetMapping("/templates")
    public ApiResponse<List<ScriptTemplateVO>> listTemplates() {
        return ApiResponse.success(scriptAssistantService.listTemplates());
    }

    @PostMapping("/generate")
    @RateLimit(max = 10, windowSeconds = 60, key = "ai:script")
    public ApiResponse<ScriptGeneration> generate(@Valid @RequestBody GenerateScriptRequest request) {
        return ApiResponse.success(scriptAssistantService.generate(request));
    }

    @GetMapping("/history")
    public ApiResponse<Page<ScriptGeneration>> history(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(scriptAssistantService.listHistory(page, size));
    }

    @PostMapping("/{id}/rate")
    public ApiResponse<Void> rate(@PathVariable Long id, @RequestParam int rating) {
        scriptAssistantService.rate(id, rating);
        return ApiResponse.success();
    }
}
