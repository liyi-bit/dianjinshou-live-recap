package com.dianjinshou.modules.recording.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.dianjinshou.common.entity.BaseEntity;

import java.time.LocalDateTime;

@TableName("recordings")
public class Recording extends BaseEntity {

    private Long streamerId;
    private Long userId;
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

    public Long getStreamerId() {
        return streamerId;
    }

    public void setStreamerId(Long streamerId) {
        this.streamerId = streamerId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getOrgId() {
        return orgId;
    }

    public void setOrgId(Long orgId) {
        this.orgId = orgId;
    }

    public String getLocalFilePath() {
        return localFilePath;
    }

    public void setLocalFilePath(String localFilePath) {
        this.localFilePath = localFilePath;
    }

    public String getLocalFileName() {
        return localFileName;
    }

    public void setLocalFileName(String localFileName) {
        this.localFileName = localFileName;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public void setStorageKey(String storageKey) {
        this.storageKey = storageKey;
    }

    public String getStreamUrl() {
        return streamUrl;
    }

    public void setStreamUrl(String streamUrl) {
        this.streamUrl = streamUrl;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getResolution() {
        return resolution;
    }

    public void setResolution(String resolution) {
        this.resolution = resolution;
    }

    public Integer getSegmentIndex() {
        return segmentIndex;
    }

    public void setSegmentIndex(Integer segmentIndex) {
        this.segmentIndex = segmentIndex;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getCoreData() {
        return coreData;
    }

    public void setCoreData(String coreData) {
        this.coreData = coreData;
    }

    public Integer getSensitiveWordCount() {
        return sensitiveWordCount;
    }

    public void setSensitiveWordCount(Integer sensitiveWordCount) {
        this.sensitiveWordCount = sensitiveWordCount;
    }

    public Integer getOperationKeywordCount() {
        return operationKeywordCount;
    }

    public void setOperationKeywordCount(Integer operationKeywordCount) {
        this.operationKeywordCount = operationKeywordCount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getAnalysisStatus() {
        return analysisStatus;
    }

    public void setAnalysisStatus(String analysisStatus) {
        this.analysisStatus = analysisStatus;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }
}
