package com.dianjinshou.modules.recap.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

public class CreateClipAnalysisRequest {

    @NotNull(message = "录制ID不能为空")
    private Long recordingId;
    @NotNull(message = "切片开始时间不能为空")
    private Integer clipStart;
    @NotNull(message = "切片结束时间不能为空")
    private Integer clipEnd;
    @NotBlank(message = "切片分类不能为空")
    private String clipCategory;
    private String clipFilename;
    private String clipFilePath;
    private String clipRemark;
    private String aiModel;

    public Long getRecordingId() {
        return recordingId;
    }

    public void setRecordingId(Long recordingId) {
        this.recordingId = recordingId;
    }

    public Integer getClipStart() {
        return clipStart;
    }

    public void setClipStart(Integer clipStart) {
        this.clipStart = clipStart;
    }

    public Integer getClipEnd() {
        return clipEnd;
    }

    public void setClipEnd(Integer clipEnd) {
        this.clipEnd = clipEnd;
    }

    public String getClipCategory() {
        return clipCategory;
    }

    public void setClipCategory(String clipCategory) {
        this.clipCategory = clipCategory;
    }

    public String getClipFilename() {
        return clipFilename;
    }

    public void setClipFilename(String clipFilename) {
        this.clipFilename = clipFilename;
    }

    public String getClipFilePath() {
        return clipFilePath;
    }

    public void setClipFilePath(String clipFilePath) {
        this.clipFilePath = clipFilePath;
    }

    public String getClipRemark() {
        return clipRemark;
    }

    public void setClipRemark(String clipRemark) {
        this.clipRemark = clipRemark;
    }

    public String getAiModel() {
        return aiModel;
    }

    public void setAiModel(String aiModel) {
        this.aiModel = aiModel;
    }
}
