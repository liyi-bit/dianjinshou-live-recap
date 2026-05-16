package com.dianjinshou.modules.storage.vo;

import java.time.LocalDateTime;

public class CloudComparisonSourceStatusVO {

    private Long comparisonId;
    private String mode;
    private Source optimize;
    private Source reference;

    public Long getComparisonId() {
        return comparisonId;
    }

    public void setComparisonId(Long comparisonId) {
        this.comparisonId = comparisonId;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public Source getOptimize() {
        return optimize;
    }

    public void setOptimize(Source optimize) {
        this.optimize = optimize;
    }

    public Source getReference() {
        return reference;
    }

    public void setReference(Source reference) {
        this.reference = reference;
    }

    public static class Source {
        private String role;
        private Long recordingId;
        private Long taskId;
        private Long streamerId;
        private String anchorName;
        private Long industryId;
        private String accountType;
        private LocalDateTime recordedAt;
        private Integer durationSeconds;
        private String fileName;
        private String localFilePath;
        private String businessType;
        private Long businessId;
        private Long clipId;
        private Long cloudFileId;
        private String cloudStatus;
        private Boolean uploaded;
        private Boolean uploading;

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public Long getRecordingId() {
            return recordingId;
        }

        public void setRecordingId(Long recordingId) {
            this.recordingId = recordingId;
        }

        public Long getTaskId() {
            return taskId;
        }

        public void setTaskId(Long taskId) {
            this.taskId = taskId;
        }

        public Long getStreamerId() {
            return streamerId;
        }

        public void setStreamerId(Long streamerId) {
            this.streamerId = streamerId;
        }

        public String getAnchorName() {
            return anchorName;
        }

        public void setAnchorName(String anchorName) {
            this.anchorName = anchorName;
        }

        public Long getIndustryId() {
            return industryId;
        }

        public void setIndustryId(Long industryId) {
            this.industryId = industryId;
        }

        public String getAccountType() {
            return accountType;
        }

        public void setAccountType(String accountType) {
            this.accountType = accountType;
        }

        public LocalDateTime getRecordedAt() {
            return recordedAt;
        }

        public void setRecordedAt(LocalDateTime recordedAt) {
            this.recordedAt = recordedAt;
        }

        public Integer getDurationSeconds() {
            return durationSeconds;
        }

        public void setDurationSeconds(Integer durationSeconds) {
            this.durationSeconds = durationSeconds;
        }

        public String getFileName() {
            return fileName;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }

        public String getLocalFilePath() {
            return localFilePath;
        }

        public void setLocalFilePath(String localFilePath) {
            this.localFilePath = localFilePath;
        }

        public String getBusinessType() {
            return businessType;
        }

        public void setBusinessType(String businessType) {
            this.businessType = businessType;
        }

        public Long getBusinessId() {
            return businessId;
        }

        public void setBusinessId(Long businessId) {
            this.businessId = businessId;
        }

        public Long getClipId() {
            return clipId;
        }

        public void setClipId(Long clipId) {
            this.clipId = clipId;
        }

        public Long getCloudFileId() {
            return cloudFileId;
        }

        public void setCloudFileId(Long cloudFileId) {
            this.cloudFileId = cloudFileId;
        }

        public String getCloudStatus() {
            return cloudStatus;
        }

        public void setCloudStatus(String cloudStatus) {
            this.cloudStatus = cloudStatus;
        }

        public Boolean getUploaded() {
            return uploaded;
        }

        public void setUploaded(Boolean uploaded) {
            this.uploaded = uploaded;
        }

        public Boolean getUploading() {
            return uploading;
        }

        public void setUploading(Boolean uploading) {
            this.uploading = uploading;
        }
    }
}
