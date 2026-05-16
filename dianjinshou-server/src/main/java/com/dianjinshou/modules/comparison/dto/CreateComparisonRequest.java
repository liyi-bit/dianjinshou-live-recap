package com.dianjinshou.modules.comparison.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

public class CreateComparisonRequest {

    @NotNull(message = "优化场次录制ID不能为空")
    private Long recordingIdOptimize;

    @NotNull(message = "参考场次录制ID不能为空")
    private Long recordingIdReference;

    @NotBlank(message = "对比类型不能为空")
    private String type;

    private String clipCategory;
    private String aiModel;

    public Long getRecordingIdOptimize() {
        return recordingIdOptimize;
    }

    public void setRecordingIdOptimize(Long recordingIdOptimize) {
        this.recordingIdOptimize = recordingIdOptimize;
    }

    public Long getRecordingIdReference() {
        return recordingIdReference;
    }

    public void setRecordingIdReference(Long recordingIdReference) {
        this.recordingIdReference = recordingIdReference;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getClipCategory() {
        return clipCategory;
    }

    public void setClipCategory(String clipCategory) {
        this.clipCategory = clipCategory;
    }

    public String getAiModel() {
        return aiModel;
    }

    public void setAiModel(String aiModel) {
        this.aiModel = aiModel;
    }
}
