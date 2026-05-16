package com.dianjinshou.modules.recording.vo;

import com.dianjinshou.modules.recording.entity.Recording;

import java.time.LocalDateTime;

public class RecordingVO {

    private Long id;
    private Long streamerId;
    private Long userId;
    private Long orgId;
    private String localFilePath;
    private String localFileName;
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
    /** 顶级冗余字段：方便前端列表组件直接取用，避免每次走 streamerInfo 嵌套 */
    private String anchorName;
    private String anchorAvatar;
    /** 与 list 接口对齐：详情接口也回传最近一次 full 类型 AnalysisTask 的 id，前端跳转 AI 助手时直接带 taskId */
    private Long latestTaskId;
    private StreamerInfo streamerInfo;

    public static RecordingVO fromEntity(Recording r) {
        RecordingVO vo = new RecordingVO();
        vo.setId(r.getId());
        vo.setStreamerId(r.getStreamerId());
        vo.setUserId(r.getUserId());
        vo.setOrgId(r.getOrgId());
        vo.setLocalFilePath(r.getLocalFilePath());
        vo.setLocalFileName(r.getLocalFileName());
        vo.setStreamUrl(r.getStreamUrl());
        vo.setStartTime(r.getStartTime());
        vo.setEndTime(r.getEndTime());
        vo.setDuration(r.getDuration());
        vo.setFileSize(r.getFileSize());
        vo.setResolution(r.getResolution());
        vo.setSegmentIndex(r.getSegmentIndex());
        vo.setSessionId(r.getSessionId());
        vo.setCoreData(r.getCoreData());
        vo.setSensitiveWordCount(r.getSensitiveWordCount());
        vo.setOperationKeywordCount(r.getOperationKeywordCount());
        vo.setStatus(r.getStatus());
        vo.setAnalysisStatus(r.getAnalysisStatus());
        vo.setErrorMsg(r.getErrorMsg());
        vo.setCreatedAt(r.getCreatedAt());
        vo.setUpdatedAt(r.getUpdatedAt());
        return vo;
    }

    public static class StreamerInfo {

        private Long id;
        private String anchorName;
        private String accountType;
        private String anchorAvatar;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getAnchorName() {
            return anchorName;
        }

        public void setAnchorName(String anchorName) {
            this.anchorName = anchorName;
        }

        public String getAccountType() {
            return accountType;
        }

        public void setAccountType(String accountType) {
            this.accountType = accountType;
        }

        public String getAnchorAvatar() {
            return anchorAvatar;
        }

        public void setAnchorAvatar(String anchorAvatar) {
            this.anchorAvatar = anchorAvatar;
        }
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public StreamerInfo getStreamerInfo() {
        return streamerInfo;
    }

    public void setStreamerInfo(StreamerInfo streamerInfo) {
        this.streamerInfo = streamerInfo;
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

    public Long getLatestTaskId() {
        return latestTaskId;
    }

    public void setLatestTaskId(Long latestTaskId) {
        this.latestTaskId = latestTaskId;
    }
}
