package com.dianjinshou.modules.ai.controller;

import com.dianjinshou.common.response.ApiResponse;
import com.dianjinshou.common.response.PageResult;
import com.dianjinshou.common.security.RateLimit;
import com.dianjinshou.modules.ai.dto.ChatRequest;
import com.dianjinshou.modules.ai.dto.ModelSwitchRequest;
import com.dianjinshou.modules.ai.service.AiChatService;
import com.dianjinshou.modules.ai.vo.ChatMessageVO;
import com.dianjinshou.modules.ai.vo.PresetQuestionVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/ai")
public class AiChatController {

    private final AiChatService aiChatService;

    public AiChatController(AiChatService aiChatService) {
        this.aiChatService = aiChatService;
    }

    @PostMapping("/chat")
    @RateLimit(max = 10, windowSeconds = 60, key = "ai:chat")
    public ApiResponse<ChatMessageVO> chat(@Valid @RequestBody ChatRequest req) {
        // Note: SSE streaming will be implemented when actual AI clients are integrated.
        // Currently returns synchronous mock response.
        return ApiResponse.success(aiChatService.chat(req));
    }

    @GetMapping("/presets/{type}")
    public ApiResponse<List<PresetQuestionVO>> getPresets(@PathVariable String type) {
        return ApiResponse.success(aiChatService.getPresets(type));
    }

    @PostMapping("/model/switch")
    public ApiResponse<Map<String, String>> switchModel(@Valid @RequestBody ModelSwitchRequest req) {
        return ApiResponse.success(aiChatService.switchModel(req.getAiModel()));
    }

    @GetMapping("/history")
    public ApiResponse<PageResult<ChatMessageVO>> getHistory(
            @RequestParam(required = false) Long taskId,
            @RequestParam(required = false) Long comparisonId,
            @RequestParam(required = false) String assistantType,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(aiChatService.getHistory(taskId, comparisonId, assistantType, page, size));
    }
}
