package com.dianjinshou.modules.obs.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("error_logs")
public class ErrorLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private Long orgId;
    private String level;          // error|warn|fatal
    private String scope;          // asr|ai|record|updater|main|renderer|api|...
    private String source;         // desktop-main|desktop-renderer|server
    private String clientVersion;
    private String platform;
    private String userAgent;
    private String message;
    private String stack;
    private Long recordingId;
    private Long taskId;
    private String modelVersion;
    private String details;        // JSON
    private String breadcrumbs;    // JSON
    private String ip;
    private LocalDateTime occurredAt;
    private LocalDateTime receivedAt;

    // getters/setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getOrgId() { return orgId; }
    public void setOrgId(Long orgId) { this.orgId = orgId; }
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
    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
    public String getBreadcrumbs() { return breadcrumbs; }
    public void setBreadcrumbs(String breadcrumbs) { this.breadcrumbs = breadcrumbs; }
    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
    public void setOccurredAt(LocalDateTime occurredAt) { this.occurredAt = occurredAt; }
    public LocalDateTime getReceivedAt() { return receivedAt; }
    public void setReceivedAt(LocalDateTime receivedAt) { this.receivedAt = receivedAt; }
}
