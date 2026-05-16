package com.dianjinshou.modules.shortclip.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import javax.validation.constraints.Size;

public class CreateShortClipRequest {

    @NotNull(message = "录制ID不能为空")
    private Long recordingId;

    @NotNull(message = "起始时间不能为空")
    @Positive
    private Integer startTime;

    @NotNull(message = "结束时间不能为空")
    @Positive
    private Integer endTime;

    @NotBlank(message = "切片名称不能为空")
    @Size(max = 15, message = "切片名称不能超过15个字符")
    private String clipName;

    private String resolution;
    private String watermarkText;

    public Long getRecordingId() { return recordingId; }
    public void setRecordingId(Long recordingId) { this.recordingId = recordingId; }

    public Integer getStartTime() { return startTime; }
    public void setStartTime(Integer startTime) { this.startTime = startTime; }

    public Integer getEndTime() { return endTime; }
    public void setEndTime(Integer endTime) { this.endTime = endTime; }

    public String getClipName() { return clipName; }
    public void setClipName(String clipName) { this.clipName = clipName; }

    public String getResolution() { return resolution; }
    public void setResolution(String resolution) { this.resolution = resolution; }

    public String getWatermarkText() { return watermarkText; }
    public void setWatermarkText(String watermarkText) { this.watermarkText = watermarkText; }
}
