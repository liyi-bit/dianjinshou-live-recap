package com.dianjinshou.modules.ai.dto;

import javax.validation.constraints.NotBlank;

public class ChatRequest {

    private Long taskId;
    private Long comparisonId;
    @NotBlank(message = "助手类型不能为空")
    private String assistantType;
    @NotBlank(message = "消息内容不能为空")
    private String message;
    private String aiModel;
    private Integer presetQuestionId;

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public Long getComparisonId() {
        return comparisonId;
    }

    public void setComparisonId(Long comparisonId) {
        this.comparisonId = comparisonId;
    }

    public String getAssistantType() {
        return assistantType;
    }

    public void setAssistantType(String assistantType) {
        this.assistantType = assistantType;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getAiModel() {
        return aiModel;
    }

    public void setAiModel(String aiModel) {
        this.aiModel = aiModel;
    }

    public Integer getPresetQuestionId() {
        return presetQuestionId;
    }

    public void setPresetQuestionId(Integer presetQuestionId) {
        this.presetQuestionId = presetQuestionId;
    }
}
