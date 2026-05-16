package com.dianjinshou.modules.shortclip.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.dianjinshou.common.entity.BaseEntity;

@TableName("short_clips")
public class ShortClip extends BaseEntity {

    private Long userId;
    private Long orgId;
    private Long recordingId;
    private String sourceType;
    private Long sourceId;
    private String clipName;
    private Integer startTime;
    private Integer endTime;
    private Integer duration;
    private String resolution;
    private String watermarkText;
    private String outputFormat;
    private String localFilePath;
    private String storageKey;
    private Long fileSize;
    private String status;
    private String errorMsg;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getOrgId() { return orgId; }
    public void setOrgId(Long orgId) { this.orgId = orgId; }

    public Long getRecordingId() { return recordingId; }
    public void setRecordingId(Long recordingId) { this.recordingId = recordingId; }

    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }

    public Long getSourceId() { return sourceId; }
    public void setSourceId(Long sourceId) { this.sourceId = sourceId; }

    public String getClipName() { return clipName; }
    public void setClipName(String clipName) { this.clipName = clipName; }

    public Integer getStartTime() { return startTime; }
    public void setStartTime(Integer startTime) { this.startTime = startTime; }

    public Integer getEndTime() { return endTime; }
    public void setEndTime(Integer endTime) { this.endTime = endTime; }

    public Integer getDuration() { return duration; }
    public void setDuration(Integer duration) { this.duration = duration; }

    public String getResolution() { return resolution; }
    public void setResolution(String resolution) { this.resolution = resolution; }

    public String getWatermarkText() { return watermarkText; }
    public void setWatermarkText(String watermarkText) { this.watermarkText = watermarkText; }

    public String getOutputFormat() { return outputFormat; }
    public void setOutputFormat(String outputFormat) { this.outputFormat = outputFormat; }

    public String getLocalFilePath() { return localFilePath; }
    public void setLocalFilePath(String localFilePath) { this.localFilePath = localFilePath; }

    public String getStorageKey() { return storageKey; }
    public void setStorageKey(String storageKey) { this.storageKey = storageKey; }

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getErrorMsg() { return errorMsg; }
    public void setErrorMsg(String errorMsg) { this.errorMsg = errorMsg; }
}
