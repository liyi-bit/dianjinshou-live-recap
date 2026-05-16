package com.dianjinshou.modules.ai.dto;

import javax.validation.constraints.NotNull;

public class GenerateScriptRequest {

    @NotNull(message = "模板ID不能为空")
    private Long templateId;

    private String inputParams;

    public Long getTemplateId() { return templateId; }
    public void setTemplateId(Long templateId) { this.templateId = templateId; }

    public String getInputParams() { return inputParams; }
    public void setInputParams(String inputParams) { this.inputParams = inputParams; }
}
