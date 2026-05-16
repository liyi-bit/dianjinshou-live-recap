package com.dianjinshou.modules.ai.dto;

import javax.validation.constraints.NotBlank;

public class ModelSwitchRequest {

    @NotBlank(message = "AI模型不能为空")
    private String aiModel;

    public String getAiModel() {
        return aiModel;
    }

    public void setAiModel(String aiModel) {
        this.aiModel = aiModel;
    }
}
