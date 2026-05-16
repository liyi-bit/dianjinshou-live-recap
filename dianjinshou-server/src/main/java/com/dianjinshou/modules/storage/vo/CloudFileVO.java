package com.dianjinshou.modules.storage.vo;

import com.dianjinshou.modules.storage.entity.CloudFile;

import java.time.LocalDateTime;

public class CloudFileVO {

    private Long id;
    private String fileName;
    private Long fileSize;
    private String contentType;
    private String fileType;
    private String businessType;
    private Long businessId;
    private Long recordingId;
    private Long clipId;
    private Long comparisonId;
    private Long streamerId;
    private String anchorName;
    private String anchorAvatar;
    private String anchorNameOptimize;
    private String anchorNameReference;
    private String anchorAvatarOptimize;
    private String anchorAvatarReference;
    private Long industryId;
    private String accountType;
    private String uploadAccount;
    private LocalDateTime recordedAt;
    private Integer durationSeconds;
    private String displayName;
    private Boolean localExists;
    private Boolean readonlyRestored;
    private Integer uploadProgress;
    private Integer downloadCount;
    private Integer shareCount;
    private String status;
    private LocalDateTime createdAt;

    public static CloudFileVO fromEntity(CloudFile entity) {
        CloudFileVO vo = new CloudFileVO();
        vo.setId(entity.getId());
        vo.setFileName(entity.getFileName());
        vo.setFileSize(entity.getFileSize());
        vo.setContentType(entity.getContentType());
        vo.setFileType(entity.getFileType());
        vo.setBusinessType(entity.getBusinessType());
        vo.setBusinessId(entity.getBusinessId());
        vo.setRecordingId(entity.getRecordingId());
        vo.setClipId(entity.getClipId());
        vo.setComparisonId(entity.getComparisonId());
        vo.setStreamerId(entity.getStreamerId());
        vo.setAnchorName(entity.getAnchorName());
        vo.setIndustryId(entity.getIndustryId());
        vo.setAccountType(entity.getAccountType());
        vo.setUploadAccount(entity.getUploadAccount());
        vo.setRecordedAt(entity.getRecordedAt());
        vo.setDurationSeconds(entity.getDurationSeconds());
        vo.setDisplayName(entity.getDisplayName());
        vo.setLocalExists(entity.getLocalExists());
        vo.setReadonlyRestored(entity.getReadonlyRestored());
        vo.setUploadProgress(entity.getUploadProgress());
        vo.setDownloadCount(entity.getDownloadCount());
        vo.setShareCount(entity.getShareCount());
        vo.setStatus(entity.getStatus());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }

    public String getBusinessType() { return businessType; }
    public void setBusinessType(String businessType) { this.businessType = businessType; }

    public Long getBusinessId() { return businessId; }
    public void setBusinessId(Long businessId) { this.businessId = businessId; }

    public Long getRecordingId() { return recordingId; }
    public void setRecordingId(Long recordingId) { this.recordingId = recordingId; }

    public Long getClipId() { return clipId; }
    public void setClipId(Long clipId) { this.clipId = clipId; }

    public Long getComparisonId() { return comparisonId; }
    public void setComparisonId(Long comparisonId) { this.comparisonId = comparisonId; }

    public Long getStreamerId() { return streamerId; }
    public void setStreamerId(Long streamerId) { this.streamerId = streamerId; }

    public String getAnchorName() { return anchorName; }
    public void setAnchorName(String anchorName) { this.anchorName = anchorName; }

    public String getAnchorAvatar() { return anchorAvatar; }
    public void setAnchorAvatar(String anchorAvatar) { this.anchorAvatar = anchorAvatar; }

    public String getAnchorNameOptimize() { return anchorNameOptimize; }
    public void setAnchorNameOptimize(String anchorNameOptimize) { this.anchorNameOptimize = anchorNameOptimize; }

    public String getAnchorNameReference() { return anchorNameReference; }
    public void setAnchorNameReference(String anchorNameReference) { this.anchorNameReference = anchorNameReference; }

    public String getAnchorAvatarOptimize() { return anchorAvatarOptimize; }
    public void setAnchorAvatarOptimize(String anchorAvatarOptimize) { this.anchorAvatarOptimize = anchorAvatarOptimize; }

    public String getAnchorAvatarReference() { return anchorAvatarReference; }
    public void setAnchorAvatarReference(String anchorAvatarReference) { this.anchorAvatarReference = anchorAvatarReference; }

    public Long getIndustryId() { return industryId; }
    public void setIndustryId(Long industryId) { this.industryId = industryId; }

    public String getAccountType() { return accountType; }
    public void setAccountType(String accountType) { this.accountType = accountType; }

    public String getUploadAccount() { return uploadAccount; }
    public void setUploadAccount(String uploadAccount) { this.uploadAccount = uploadAccount; }

    public LocalDateTime getRecordedAt() { return recordedAt; }
    public void setRecordedAt(LocalDateTime recordedAt) { this.recordedAt = recordedAt; }

    public Integer getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(Integer durationSeconds) { this.durationSeconds = durationSeconds; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public Boolean getLocalExists() { return localExists; }
    public void setLocalExists(Boolean localExists) { this.localExists = localExists; }

    public Boolean getReadonlyRestored() { return readonlyRestored; }
    public void setReadonlyRestored(Boolean readonlyRestored) { this.readonlyRestored = readonlyRestored; }

    public Integer getUploadProgress() { return uploadProgress; }
    public void setUploadProgress(Integer uploadProgress) { this.uploadProgress = uploadProgress; }

    public Integer getDownloadCount() { return downloadCount; }
    public void setDownloadCount(Integer downloadCount) { this.downloadCount = downloadCount; }

    public Integer getShareCount() { return shareCount; }
    public void setShareCount(Integer shareCount) { this.shareCount = shareCount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
