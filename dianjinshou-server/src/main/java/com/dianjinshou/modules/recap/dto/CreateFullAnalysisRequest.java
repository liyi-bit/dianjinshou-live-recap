package com.dianjinshou.modules.recap.dto;

import javax.validation.constraints.NotNull;

public class CreateFullAnalysisRequest {

    @NotNull(message = "录制ID不能为空")
    private Long recordingId;
    private String industry;
    private String aiModel;

    public Long getRecordingId() {
        return recordingId;
    }

    public void setRecordingId(Long recordingId) {
        this.recordingId = recordingId;
    }

    public String getIndustry() {
        return industry;
    }

    public void setIndustry(String industry) {
        this.industry = industry;
    }

    public String getAiModel() {
        return aiModel;
    }

    public void setAiModel(String aiModel) {
        this.aiModel = aiModel;
    }
}
