package com.dianjinshou.modules.admin.vo;

import java.time.LocalDateTime;

public class AdminTaskDetailVO {

    private Long id;
    private String taskType;
    private Long userId;
    private String username;
    private Long orgId;
    private Long recordingId;
    private String recordingName;
    private String subType;
    private String status;
    private Integer priority;
    private String aiModel;
    private String industry;
    private String fileName;
    private Long fileSize;
    private Integer duration;
    private Integer totalParts;
    private Integer uploadedParts;
    private String storageKey;
    private String asrText;
    private Integer asrWordCount;
    private String aiResult;
    private String aiDiagnosis;
    private String keywordSummary;
    private String sensitiveWords;
    private Integer sensitiveCount;
    private String contentCompass;
    private String optimizedText;
    private String optimizationAction;
    private String optimizationGoal;
    private String summary;
    private Long consumedChars;
    private String errorMsg;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTaskType() { return taskType; }
    public void setTaskType(String taskType) { this.taskType = taskType; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public Long getOrgId() { return orgId; }
    public void setOrgId(Long orgId) { this.orgId = orgId; }
    public Long getRecordingId() { return recordingId; }
    public void setRecordingId(Long recordingId) { this.recordingId = recordingId; }
    public String getRecordingName() { return recordingName; }
    public void setRecordingName(String recordingName) { this.recordingName = recordingName; }
    public String getSubType() { return subType; }
    public void setSubType(String subType) { this.subType = subType; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }
    public String getAiModel() { return aiModel; }
    public void setAiModel(String aiModel) { this.aiModel = aiModel; }
    public String getIndustry() { return industry; }
    public void setIndustry(String industry) { this.industry = industry; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    public Integer getDuration() { return duration; }
    public void setDuration(Integer duration) { this.duration = duration; }
    public Integer getTotalParts() { return totalParts; }
    public void setTotalParts(Integer totalParts) { this.totalParts = totalParts; }
    public Integer getUploadedParts() { return uploadedParts; }
    public void setUploadedParts(Integer uploadedParts) { this.uploadedParts = uploadedParts; }
    public String getStorageKey() { return storageKey; }
    public void setStorageKey(String storageKey) { this.storageKey = storageKey; }
    public String getAsrText() { return asrText; }
    public void setAsrText(String asrText) { this.asrText = asrText; }
    public Integer getAsrWordCount() { return asrWordCount; }
    public void setAsrWordCount(Integer asrWordCount) { this.asrWordCount = asrWordCount; }
    public String getAiResult() { return aiResult; }
    public void setAiResult(String aiResult) { this.aiResult = aiResult; }
    public String getAiDiagnosis() { return aiDiagnosis; }
    public void setAiDiagnosis(String aiDiagnosis) { this.aiDiagnosis = aiDiagnosis; }
    public String getKeywordSummary() { return keywordSummary; }
    public void setKeywordSummary(String keywordSummary) { this.keywordSummary = keywordSummary; }
    public String getSensitiveWords() { return sensitiveWords; }
    public void setSensitiveWords(String sensitiveWords) { this.sensitiveWords = sensitiveWords; }
    public Integer getSensitiveCount() { return sensitiveCount; }
    public void setSensitiveCount(Integer sensitiveCount) { this.sensitiveCount = sensitiveCount; }
    public String getContentCompass() { return contentCompass; }
    public void setContentCompass(String contentCompass) { this.contentCompass = contentCompass; }
    public String getOptimizedText() { return optimizedText; }
    public void setOptimizedText(String optimizedText) { this.optimizedText = optimizedText; }
    public String getOptimizationAction() { return optimizationAction; }
    public void setOptimizationAction(String optimizationAction) { this.optimizationAction = optimizationAction; }
    public String getOptimizationGoal() { return optimizationGoal; }
    public void setOptimizationGoal(String optimizationGoal) { this.optimizationGoal = optimizationGoal; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public Long getConsumedChars() { return consumedChars; }
    public void setConsumedChars(Long consumedChars) { this.consumedChars = consumedChars; }
    public String getErrorMsg() { return errorMsg; }
    public void setErrorMsg(String errorMsg) { this.errorMsg = errorMsg; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
