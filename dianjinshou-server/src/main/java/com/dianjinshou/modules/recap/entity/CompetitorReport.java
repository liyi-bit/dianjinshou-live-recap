package com.dianjinshou.modules.recap.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.time.LocalDateTime;

@TableName("competitor_reports")
public class CompetitorReport {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long orgId;
    private Long streamerId;
    private Long competitorStreamerId;
    private Long recordingId;
    private Long competitorRecordingId;
    private String report;
    private String aiModel;
    private String status;
    private String errorMsg;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getOrgId() { return orgId; }
    public void setOrgId(Long orgId) { this.orgId = orgId; }

    public Long getStreamerId() { return streamerId; }
    public void setStreamerId(Long streamerId) { this.streamerId = streamerId; }

    public Long getCompetitorStreamerId() { return competitorStreamerId; }
    public void setCompetitorStreamerId(Long competitorStreamerId) { this.competitorStreamerId = competitorStreamerId; }

    public Long getRecordingId() { return recordingId; }
    public void setRecordingId(Long recordingId) { this.recordingId = recordingId; }

    public Long getCompetitorRecordingId() { return competitorRecordingId; }
    public void setCompetitorRecordingId(Long competitorRecordingId) { this.competitorRecordingId = competitorRecordingId; }

    public String getReport() { return report; }
    public void setReport(String report) { this.report = report; }

    public String getAiModel() { return aiModel; }
    public void setAiModel(String aiModel) { this.aiModel = aiModel; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getErrorMsg() { return errorMsg; }
    public void setErrorMsg(String errorMsg) { this.errorMsg = errorMsg; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public Integer getDeleted() { return deleted; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
