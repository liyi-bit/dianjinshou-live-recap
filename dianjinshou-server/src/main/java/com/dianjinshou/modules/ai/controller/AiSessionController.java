package com.dianjinshou.modules.ai.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dianjinshou.common.response.ApiResponse;
import com.dianjinshou.modules.ai.dto.CreateSessionRequest;
import com.dianjinshou.modules.ai.dto.SendMessageRequest;
import com.dianjinshou.modules.ai.service.AiSessionService;
import com.dianjinshou.modules.ai.vo.AiSessionDetailVO;
import com.dianjinshou.modules.ai.vo.AiSessionVO;
import com.dianjinshou.modules.ai.vo.ChatMessageVO;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/ai/sessions")
public class AiSessionController {

    private final AiSessionService aiSessionService;

    public AiSessionController(AiSessionService aiSessionService) {
        this.aiSessionService = aiSessionService;
    }

    @PostMapping
    public ApiResponse<AiSessionVO> createSession(@Valid @RequestBody CreateSessionRequest request) {
        return ApiResponse.success(aiSessionService.createSession(request));
    }

    @GetMapping
    public ApiResponse<Page<AiSessionVO>> listSessions(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String assistantType) {
        return ApiResponse.success(aiSessionService.listSessions(page, size, assistantType));
    }

    @GetMapping("/{id}")
    public ApiResponse<AiSessionDetailVO> getSession(@PathVariable Long id) {
        return ApiResponse.success(aiSessionService.getSessionDetail(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> updateTitle(@PathVariable Long id, @RequestBody Map<String, String> body) {
        aiSessionService.updateTitle(id, body.get("title"));
        return ApiResponse.success();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteSession(@PathVariable Long id) {
        aiSessionService.deleteSession(id);
        return ApiResponse.success();
    }

    @PostMapping("/{id}/messages")
    public ApiResponse<ChatMessageVO> sendMessage(@PathVariable Long id,
                                                   @Valid @RequestBody SendMessageRequest request) {
        return ApiResponse.success(aiSessionService.sendMessage(id, request));
    }
}
