package com.dianjinshou.modules.admin.vo;

import com.dianjinshou.modules.recording.entity.Recording;

import java.time.LocalDateTime;
import java.util.List;

public class AdminRecordingDetailVO {

    private Long id;
    private Long userId;
    private String username;
    private Long streamerId;
    private Long orgId;
    private String localFilePath;
    private String localFileName;
    private String storageKey;
    private String streamUrl;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer duration;
    private Long fileSize;
    private String resolution;
    private Integer segmentIndex;
    private String sessionId;
    private String coreData;
    private Integer sensitiveWordCount;
    private Integer operationKeywordCount;
    private String status;
    private String analysisStatus;
    private String errorMsg;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<AdminTaskVO> relatedTasks;

    public static AdminRecordingDetailVO fromEntity(Recording r, String username, List<AdminTaskVO> tasks) {
        AdminRecordingDetailVO vo = new AdminRecordingDetailVO();
        vo.id = r.getId();
        vo.userId = r.getUserId();
        vo.username = username;
        vo.streamerId = r.getStreamerId();
        vo.orgId = r.getOrgId();
        vo.localFilePath = r.getLocalFilePath();
        vo.localFileName = r.getLocalFileName();
        vo.storageKey = r.getStorageKey();
        vo.streamUrl = r.getStreamUrl();
        vo.startTime = r.getStartTime();
        vo.endTime = r.getEndTime();
        vo.duration = r.getDuration();
        vo.fileSize = r.getFileSize();
        vo.resolution = r.getResolution();
        vo.segmentIndex = r.getSegmentIndex();
        vo.sessionId = r.getSessionId();
        vo.coreData = r.getCoreData();
        vo.sensitiveWordCount = r.getSensitiveWordCount();
        vo.operationKeywordCount = r.getOperationKeywordCount();
        vo.status = r.getStatus();
        vo.analysisStatus = r.getAnalysisStatus();
        vo.errorMsg = r.getErrorMsg();
        vo.createdAt = r.getCreatedAt();
        vo.updatedAt = r.getUpdatedAt();
        vo.relatedTasks = tasks;
        return vo;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public Long getStreamerId() { return streamerId; }
    public void setStreamerId(Long streamerId) { this.streamerId = streamerId; }
    public Long getOrgId() { return orgId; }
    public void setOrgId(Long orgId) { this.orgId = orgId; }
    public String getLocalFilePath() { return localFilePath; }
    public void setLocalFilePath(String localFilePath) { this.localFilePath = localFilePath; }
    public String getLocalFileName() { return localFileName; }
    public void setLocalFileName(String localFileName) { this.localFileName = localFileName; }
    public String getStorageKey() { return storageKey; }
    public void setStorageKey(String storageKey) { this.storageKey = storageKey; }
    public String getStreamUrl() { return streamUrl; }
    public void setStreamUrl(String streamUrl) { this.streamUrl = streamUrl; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    public Integer getDuration() { return duration; }
    public void setDuration(Integer duration) { this.duration = duration; }
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    public String getResolution() { return resolution; }
    public void setResolution(String resolution) { this.resolution = resolution; }
    public Integer getSegmentIndex() { return segmentIndex; }
    public void setSegmentIndex(Integer segmentIndex) { this.segmentIndex = segmentIndex; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public String getCoreData() { return coreData; }
    public void setCoreData(String coreData) { this.coreData = coreData; }
    public Integer getSensitiveWordCount() { return sensitiveWordCount; }
    public void setSensitiveWordCount(Integer sensitiveWordCount) { this.sensitiveWordCount = sensitiveWordCount; }
    public Integer getOperationKeywordCount() { return operationKeywordCount; }
    public void setOperationKeywordCount(Integer operationKeywordCount) { this.operationKeywordCount = operationKeywordCount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getAnalysisStatus() { return analysisStatus; }
    public void setAnalysisStatus(String analysisStatus) { this.analysisStatus = analysisStatus; }
    public String getErrorMsg() { return errorMsg; }
    public void setErrorMsg(String errorMsg) { this.errorMsg = errorMsg; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public List<AdminTaskVO> getRelatedTasks() { return relatedTasks; }
    public void setRelatedTasks(List<AdminTaskVO> relatedTasks) { this.relatedTasks = relatedTasks; }
}
