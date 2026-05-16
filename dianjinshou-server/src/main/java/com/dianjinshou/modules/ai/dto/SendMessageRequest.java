package com.dianjinshou.modules.ai.dto;

import javax.validation.constraints.NotBlank;

public class SendMessageRequest {

    @NotBlank(message = "消息内容不能为空")
    private String content;

    private Integer presetQuestionId;

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Integer getPresetQuestionId() { return presetQuestionId; }
    public void setPresetQuestionId(Integer presetQuestionId) { this.presetQuestionId = presetQuestionId; }
}
