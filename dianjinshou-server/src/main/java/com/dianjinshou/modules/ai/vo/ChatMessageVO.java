package com.dianjinshou.modules.ai.vo;

import com.dianjinshou.modules.ai.entity.AiConversation;

import java.time.LocalDateTime;

public class ChatMessageVO {

    private Long id;
    private String role;
    private String content;
    private String thinking;
    private Integer tokensUsed;
    private Integer presetQuestionId;
    private LocalDateTime createdAt;

    public static ChatMessageVO fromEntity(AiConversation conv) {
        ChatMessageVO vo = new ChatMessageVO();
        vo.setId(conv.getId());
        vo.setRole(conv.getRole());
        vo.setContent(conv.getContent());
        vo.setThinking(conv.getThinking());
        vo.setTokensUsed(conv.getTokensUsed());
        vo.setPresetQuestionId(conv.getPresetQuestionId());
        vo.setCreatedAt(conv.getCreatedAt());
        return vo;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getThinking() {
        return thinking;
    }

    public void setThinking(String thinking) {
        this.thinking = thinking;
    }

    public Integer getTokensUsed() {
        return tokensUsed;
    }

    public void setTokensUsed(Integer tokensUsed) {
        this.tokensUsed = tokensUsed;
    }

    public Integer getPresetQuestionId() {
        return presetQuestionId;
    }

    public void setPresetQuestionId(Integer presetQuestionId) {
        this.presetQuestionId = presetQuestionId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
