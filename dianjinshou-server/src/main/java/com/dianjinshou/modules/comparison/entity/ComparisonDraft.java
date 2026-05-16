package com.dianjinshou.modules.comparison.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.time.LocalDateTime;

@TableName("comparison_drafts")
public class ComparisonDraft {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long firstRecordingId;

    private Long firstTaskId;

    private String listContext;

    private LocalDateTime expiresAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getFirstRecordingId() {
        return firstRecordingId;
    }

    public void setFirstRecordingId(Long firstRecordingId) {
        this.firstRecordingId = firstRecordingId;
    }

    public Long getFirstTaskId() {
        return firstTaskId;
    }

    public void setFirstTaskId(Long firstTaskId) {
        this.firstTaskId = firstTaskId;
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
