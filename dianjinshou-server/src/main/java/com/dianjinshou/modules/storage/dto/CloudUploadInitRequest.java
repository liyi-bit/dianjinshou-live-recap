package com.dianjinshou.modules.storage.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

public class CloudUploadInitRequest {

    @NotBlank(message = "文件名不能为空")
    private String fileName;

    @NotNull(message = "文件大小不能为空")
    @Min(value = 1, message = "文件大小必须大于0")
    private Long fileSize;

    private String contentType;

    @NotBlank(message = "业务类型不能为空")
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

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime recordedAt;

    private Integer durationSeconds;
    private String localFilePath;
    private String clientTaskId;
    private Boolean manualUpload;

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

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

    public String getLocalFilePath() { return localFilePath; }
    public void setLocalFilePath(String localFilePath) { this.localFilePath = localFilePath; }

    public String getClientTaskId() { return clientTaskId; }
    public void setClientTaskId(String clientTaskId) { this.clientTaskId = clientTaskId; }

    public Boolean getManualUpload() { return manualUpload; }
    public void setManualUpload(Boolean manualUpload) { this.manualUpload = manualUpload; }
}
