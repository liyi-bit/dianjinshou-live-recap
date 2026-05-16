package com.dianjinshou.modules.obs.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class ReportErrorRequest {

    private List<ErrorItem> items;

    public List<ErrorItem> getItems() { return items; }
    public void setItems(List<ErrorItem> items) { this.items = items; }

    public static class ErrorItem {
        private String level;            // error|warn|fatal（默认 error）
        private String scope;            // asr|ai|record|updater|main|renderer|api
        private String source;           // desktop-main|desktop-renderer|server
        private String clientVersion;
        private String platform;
        private String userAgent;
        private String message;
        private String stack;
        private Long recordingId;
        private Long taskId;
        private String modelVersion;
        private Map<String, Object> details;      // 任意结构化上下文
        private List<Map<String, Object>> breadcrumbs;
        private LocalDateTime occurredAt;

        public String getLevel() { return level; }
        public void setLevel(String level) { this.level = level; }
        public String getScope() { return scope; }
        public void setScope(String scope) { this.scope = scope; }
        public String getSource() { return source; }
        public void setSource(String source) { this.source = source; }
        public String getClientVersion() { return clientVersion; }
        public void setClientVersion(String clientVersion) { this.clientVersion = clientVersion; }
        public String getPlatform() { return platform; }
        public void setPlatform(String platform) { this.platform = platform; }
        public String getUserAgent() { return userAgent; }
        public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public String getStack() { return stack; }
        public void setStack(String stack) { this.stack = stack; }
        public Long getRecordingId() { return recordingId; }
        public void setRecordingId(Long recordingId) { this.recordingId = recordingId; }
        public Long getTaskId() { return taskId; }
        public void setTaskId(Long taskId) { this.taskId = taskId; }
        public String getModelVersion() { return modelVersion; }
        public void setModelVersion(String modelVersion) { this.modelVersion = modelVersion; }
        public Map<String, Object> getDetails() { return details; }
        public void setDetails(Map<String, Object> details) { this.details = details; }
        public List<Map<String, Object>> getBreadcrumbs() { return breadcrumbs; }
        public void setBreadcrumbs(List<Map<String, Object>> breadcrumbs) { this.breadcrumbs = breadcrumbs; }
        public LocalDateTime getOccurredAt() { return occurredAt; }
        public void setOccurredAt(LocalDateTime occurredAt) { this.occurredAt = occurredAt; }
    }
}
