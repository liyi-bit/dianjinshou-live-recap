package com.dianjinshou.modules.feishu.controller;

import com.dianjinshou.common.exception.BusinessException;
import com.dianjinshou.common.response.ApiResponse;
import com.dianjinshou.common.response.ErrorCode;
import com.dianjinshou.common.security.SecurityContextHelper;
import com.dianjinshou.modules.feishu.service.FeishuTaskDispatcher;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Endpoints consumed by the desktop long-poll worker.
 *   GET  /api/v1/feishu/tasks/next?wait=25      - long poll for next task (per current user)
 *   POST /api/v1/feishu/tasks/{taskId}/result   - deliver parsed result back
 */
@RestController
@RequestMapping("/api/v1/feishu/tasks")
public class FeishuTaskController {

    private final FeishuTaskDispatcher dispatcher;

    public FeishuTaskController(FeishuTaskDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    @GetMapping("/next")
    public ApiResponse<Map<String, Object>> nextTask(@RequestParam(defaultValue = "25") int wait) throws InterruptedException {
        Long userId = SecurityContextHelper.currentUserId();
        if (userId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED);
        if (wait < 1) wait = 1;
        if (wait > 50) wait = 50;
        FeishuTaskDispatcher.Task task = dispatcher.pollNext(userId, wait);
        if (task == null) return ApiResponse.success(null);
        Map<String, Object> m = new HashMap<>();
        m.put("taskId", task.taskId);
        m.put("type", task.type);
        m.put("payload", task.payload);
        return ApiResponse.success(m);
    }

    @PostMapping("/{taskId}/result")
    public ApiResponse<Void> deliverResult(@PathVariable String taskId, @RequestBody Map<String, Object> body) {
        Boolean ok = body.get("ok") instanceof Boolean ? (Boolean) body.get("ok") : null;
        Object dataObj = body.get("data");
        String error = body.get("error") instanceof String ? (String) body.get("error") : null;

        FeishuTaskDispatcher.TaskResult result;
        if (Boolean.TRUE.equals(ok)) {
            Map<String, Object> data = dataObj instanceof Map ? (Map<String, Object>) dataObj : new HashMap<>();
            result = FeishuTaskDispatcher.TaskResult.success(data);
        } else {
            result = FeishuTaskDispatcher.TaskResult.error(error != null ? error : "desktop reported failure");
        }
        dispatcher.deliverResult(taskId, result);
        return ApiResponse.success();
    }
}
