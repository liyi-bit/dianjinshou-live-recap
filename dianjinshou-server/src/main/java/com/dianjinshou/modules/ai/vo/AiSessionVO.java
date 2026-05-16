package com.dianjinshou.modules.ai.vo;

import com.dianjinshou.modules.ai.entity.AiSession;

import java.time.LocalDateTime;

public class AiSessionVO {

    private Long id;
    private String assistantType;
    private String title;
    private Integer messageCount;
    private LocalDateTime lastMessageAt;
    private String status;
    private LocalDateTime createdAt;

    public static AiSessionVO fromEntity(AiSession entity) {
        AiSessionVO vo = new AiSessionVO();
        vo.setId(entity.getId());
        vo.setAssistantType(entity.getAssistantType());
        vo.setTitle(entity.getTitle());
        vo.setMessageCount(entity.getMessageCount());
        vo.setLastMessageAt(entity.getLastMessageAt());
        vo.setStatus(entity.getStatus());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getAssistantType() { return assistantType; }
    public void setAssistantType(String assistantType) { this.assistantType = assistantType; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public Integer getMessageCount() { return messageCount; }
    public void setMessageCount(Integer messageCount) { this.messageCount = messageCount; }

    public LocalDateTime getLastMessageAt() { return lastMessageAt; }
    public void setLastMessageAt(LocalDateTime lastMessageAt) { this.lastMessageAt = lastMessageAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
