package com.dianjinshou.modules.ai.dto;

import javax.validation.constraints.NotBlank;

public class CreateSessionRequest {

    @NotBlank(message = "助手类型不能为空")
    private String assistantType;

    private String initialMessage;

    public String getAssistantType() { return assistantType; }
    public void setAssistantType(String assistantType) { this.assistantType = assistantType; }

    public String getInitialMessage() { return initialMessage; }
    public void setInitialMessage(String initialMessage) { this.initialMessage = initialMessage; }
}
