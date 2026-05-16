package com.dianjinshou.modules.comparison.vo;

import com.dianjinshou.modules.comparison.entity.ComparisonDraft;

import java.time.LocalDateTime;

public class ComparisonDraftVO {

    private Long id;
    private Long firstRecordingId;
    private String listContext;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;

    public static ComparisonDraftVO fromEntity(ComparisonDraft entity) {
        if (entity == null) return null;
        ComparisonDraftVO vo = new ComparisonDraftVO();
        vo.setId(entity.getId());
        vo.setFirstRecordingId(entity.getFirstRecordingId());
        vo.setListContext(entity.getListContext());
        vo.setExpiresAt(entity.getExpiresAt());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getFirstRecordingId() {
        return firstRecordingId;
    }

    public void setFirstRecordingId(Long firstRecordingId) {
        this.firstRecordingId = firstRecordingId;
    }

    public String getListContext() {
        return listContext;
    }

    public void setListContext(String listContext) {
        this.listContext = listContext;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
