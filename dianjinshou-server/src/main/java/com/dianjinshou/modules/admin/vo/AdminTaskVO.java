package com.dianjinshou.modules.admin.vo;

import java.time.LocalDateTime;

public class AdminTaskVO {

    private Long id;
    private String taskType;
    private Long userId;
    private String username;
    private String userPhone;
    private Long streamerId;
    private String streamerName;
    private String streamerAvatar;
    private Long orgId;
    private String subType;
    private String status;
    private String aiModel;
    private String errorMsg;
    private String resource;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTaskType() { return taskType; }
    public void setTaskType(String taskType) { this.taskType = taskType; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getUserPhone() { return userPhone; }
    public void setUserPhone(String userPhone) { this.userPhone = userPhone; }
    public Long getStreamerId() { return streamerId; }
    public void setStreamerId(Long streamerId) { this.streamerId = streamerId; }
    public String getStreamerName() { return streamerName; }
    public void setStreamerName(String streamerName) { this.streamerName = streamerName; }
    public String getStreamerAvatar() { return streamerAvatar; }
    public void setStreamerAvatar(String streamerAvatar) { this.streamerAvatar = streamerAvatar; }
    public Long getOrgId() { return orgId; }
    public void setOrgId(Long orgId) { this.orgId = orgId; }
    public String getSubType() { return subType; }
    public void setSubType(String subType) { this.subType = subType; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getAiModel() { return aiModel; }
    public void setAiModel(String aiModel) { this.aiModel = aiModel; }
    public String getErrorMsg() { return errorMsg; }
    public void setErrorMsg(String errorMsg) { this.errorMsg = errorMsg; }
    public String getResource() { return resource; }
    public void setResource(String resource) { this.resource = resource; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
