package com.dianjinshou.modules.ai.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.dianjinshou.common.entity.BaseEntity;

import java.time.LocalDateTime;

@TableName("ai_sessions")
public class AiSession extends BaseEntity {

    private Long userId;
    private Long orgId;
    private String assistantType;
    private String title;
    private Integer messageCount;
    private LocalDateTime lastMessageAt;
    private String status;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getOrgId() { return orgId; }
    public void setOrgId(Long orgId) { this.orgId = orgId; }

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
}
