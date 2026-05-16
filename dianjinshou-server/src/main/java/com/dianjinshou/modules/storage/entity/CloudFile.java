package com.dianjinshou.modules.storage.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.dianjinshou.common.entity.BaseEntity;

import java.time.LocalDateTime;

@TableName("cloud_files")
public class CloudFile extends BaseEntity {

    private Long userId;
    private Long orgId;
    private String fileName;
    private String storageKey;
    private String bucket;
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
    private Long industryId;
    private String accountType;
    private String uploadAccount;
    private LocalDateTime recordedAt;
    private Integer durationSeconds;
    private String displayName;
    private Boolean localExists;
    private Boolean readonlyRestored;
    private Integer uploadProgress;
    private Long sourceId;
    private String checksum;
    private Integer downloadCount;
    private Integer shareCount;
    private String status;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getOrgId() { return orgId; }
    public void setOrgId(Long orgId) { this.orgId = orgId; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getStorageKey() { return storageKey; }
    public void setStorageKey(String storageKey) { this.storageKey = storageKey; }

    public String getBucket() { return bucket; }
    public void setBucket(String bucket) { this.bucket = bucket; }

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

    public Long getSourceId() { return sourceId; }
    public void setSourceId(Long sourceId) { this.sourceId = sourceId; }

    public String getChecksum() { return checksum; }
    public void setChecksum(String checksum) { this.checksum = checksum; }

    public Integer getDownloadCount() { return downloadCount; }
    public void setDownloadCount(Integer downloadCount) { this.downloadCount = downloadCount; }

    public Integer getShareCount() { return shareCount; }
    public void setShareCount(Integer shareCount) { this.shareCount = shareCount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
