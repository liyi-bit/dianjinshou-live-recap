package com.dianjinshou.modules.recording.vo;

import com.dianjinshou.modules.recording.entity.Recording;

import java.time.LocalDateTime;

public class RecordingListVO {

    private Long id;
    private Long streamerId;
    private String localFilePath;
    private String localFileName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer duration;
    private Long fileSize;
    private String resolution;
    private String status;
    private String analysisStatus;
    private Integer sensitiveWordCount;
    private Integer operationKeywordCount;
    private Long latestTaskId;
    private String anchorName;
    private String anchorAvatar;
    private LocalDateTime createdAt;

    public static RecordingListVO fromEntity(Recording r) {
        RecordingListVO vo = new RecordingListVO();
        vo.setId(r.getId());
        vo.setStreamerId(r.getStreamerId());
        vo.setLocalFilePath(r.getLocalFilePath());
        vo.setLocalFileName(r.getLocalFileName());
        vo.setStartTime(r.getStartTime());
        vo.setEndTime(r.getEndTime());
        vo.setDuration(r.getDuration());
        vo.setFileSize(r.getFileSize());
        vo.setResolution(r.getResolution());
        vo.setStatus(r.getStatus());
        vo.setAnalysisStatus(r.getAnalysisStatus());
        vo.setSensitiveWordCount(r.getSensitiveWordCount());
        vo.setOperationKeywordCount(r.getOperationKeywordCount());
        vo.setCreatedAt(r.getCreatedAt());
        return vo;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getStreamerId() {
        return streamerId;
    }

    public void setStreamerId(Long streamerId) {
        this.streamerId = streamerId;
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

    public Long getLatestTaskId() {
        return latestTaskId;
    }

    public void setLatestTaskId(Long latestTaskId) {
        this.latestTaskId = latestTaskId;
    }

    public String getAnchorName() {
        return anchorName;
    }

    public void setAnchorName(String anchorName) {
        this.anchorName = anchorName;
    }

    public String getAnchorAvatar() {
        return anchorAvatar;
    }

    public void setAnchorAvatar(String anchorAvatar) {
        this.anchorAvatar = anchorAvatar;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
