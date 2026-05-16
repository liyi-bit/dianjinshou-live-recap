package com.dianjinshou.modules.ai.vo;

import java.util.List;

public class AiSessionDetailVO extends AiSessionVO {

    private List<ChatMessageVO> messages;

    public List<ChatMessageVO> getMessages() { return messages; }
    public void setMessages(List<ChatMessageVO> messages) { this.messages = messages; }
}
