package com.dianjinshou.modules.feishu.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

/**
 * In-memory task channel between the backend (Feishu handler) and online desktop clients.
 *
 * <ul>
 *   <li>Desktop clients long-poll {@code /feishu/next-task?userId=N} and block on the per-user queue.</li>
 *   <li>When a Feishu message arrives, the handler {@link #dispatch} a task and awaits its result.</li>
 *   <li>Desktop posts the result to {@code /feishu/task-result}, which completes the waiting Future.</li>
 * </ul>
 *
 * State is intentionally in-memory — on app restart any in-flight tasks are dropped and the
 * Feishu handler times out with a friendly message.
 */
@Component
public class FeishuTaskDispatcher {

    private static final Logger log = LoggerFactory.getLogger(FeishuTaskDispatcher.class);
    private static final int DEFAULT_TASK_WAIT_SEC = 20;

    /** user_id → queue of pending tasks */
    private final Map<Long, BlockingQueue<Task>> userQueues = new ConcurrentHashMap<>();

    /** task_id → future to resolve */
    private final Map<String, CompletableFuture<TaskResult>> futures = new ConcurrentHashMap<>();

    /** Dispatch a task to the user and await the desktop's response (blocks up to {@code timeoutSec}). */
    public TaskResult dispatch(Long userId, String type, Map<String, Object> payload, int timeoutSec) {
        final String taskId = UUID.randomUUID().toString();
        Task task = new Task(taskId, type, payload);
        BlockingQueue<Task> q = userQueues.computeIfAbsent(userId, k -> new LinkedBlockingQueue<>());
        q.offer(task);

        CompletableFuture<TaskResult> future = new CompletableFuture<>();
        futures.put(taskId, future);
        log.info("FeishuTask dispatched: userId={} taskId={} type={}", userId, taskId, type);
        try {
            return future.get(timeoutSec, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            // Clean the future — if desktop eventually posts a result it just won't find the future.
            futures.remove(taskId);
            // Also try to remove from queue in case nobody picked it up.
            q.remove(task);
            return TaskResult.timeout();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return TaskResult.error("interrupted");
        } catch (ExecutionException e) {
            return TaskResult.error(e.getMessage());
        }
    }

    public TaskResult dispatch(Long userId, String type, Map<String, Object> payload) {
        return dispatch(userId, type, payload, DEFAULT_TASK_WAIT_SEC);
    }

    /**
     * Called by the desktop long-poll endpoint. Blocks until a task is available or
     * {@code waitSec} seconds pass (then returns null for HTTP 204).
     */
    public Task pollNext(Long userId, int waitSec) throws InterruptedException {
        BlockingQueue<Task> q = userQueues.computeIfAbsent(userId, k -> new LinkedBlockingQueue<>());
        return q.poll(waitSec, TimeUnit.SECONDS);
    }

    /** Called by desktop to deliver the parsed result. Idempotent — repeat calls are ignored. */
    public void deliverResult(String taskId, TaskResult result) {
        CompletableFuture<TaskResult> f = futures.remove(taskId);
        if (f == null) {
            log.warn("FeishuTask result arrived for unknown/expired taskId={}", taskId);
            return;
        }
        f.complete(result);
    }

    // ---- DTOs ----

    public static class Task {
        public final String taskId;
        public final String type;
        public final Map<String, Object> payload;

        Task(String taskId, String type, Map<String, Object> payload) {
            this.taskId = taskId;
            this.type = type;
            this.payload = payload;
        }
    }

    public static class TaskResult {
        public final boolean ok;
        public final String error;
        public final Map<String, Object> data;

        private TaskResult(boolean ok, String error, Map<String, Object> data) {
            this.ok = ok;
            this.error = error;
            this.data = data;
        }

        public static TaskResult success(Map<String, Object> data) { return new TaskResult(true, null, data); }
        public static TaskResult error(String msg) { return new TaskResult(false, msg, null); }
        public static TaskResult timeout() { return new TaskResult(false, "timeout", null); }
    }
}
