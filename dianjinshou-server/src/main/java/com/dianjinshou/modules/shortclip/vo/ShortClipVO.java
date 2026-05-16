package com.dianjinshou.modules.shortclip.vo;

import com.dianjinshou.modules.shortclip.entity.ShortClip;

import java.time.LocalDateTime;

public class ShortClipVO {

    private Long id;
    private Long recordingId;
    private String clipName;
    private Integer startTime;
    private Integer endTime;
    private Integer duration;
    private String resolution;
    private Long fileSize;
    private String status;
    private String errorMsg;
    private LocalDateTime createdAt;

    public static ShortClipVO fromEntity(ShortClip entity) {
        ShortClipVO vo = new ShortClipVO();
        vo.setId(entity.getId());
        vo.setRecordingId(entity.getRecordingId());
        vo.setClipName(entity.getClipName());
        vo.setStartTime(entity.getStartTime());
        vo.setEndTime(entity.getEndTime());
        vo.setDuration(entity.getDuration());
        vo.setResolution(entity.getResolution());
        vo.setFileSize(entity.getFileSize());
        vo.setStatus(entity.getStatus());
        vo.setErrorMsg(entity.getErrorMsg());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getRecordingId() { return recordingId; }
    public void setRecordingId(Long recordingId) { this.recordingId = recordingId; }

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

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getErrorMsg() { return errorMsg; }
    public void setErrorMsg(String errorMsg) { this.errorMsg = errorMsg; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
