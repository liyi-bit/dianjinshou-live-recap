package com.dianjinshou.modules.comparison.vo;

import com.dianjinshou.modules.comparison.entity.Comparison;

import java.time.LocalDateTime;

public class ComparisonVO {

    private Long id;
    private String type;
    private Long recordingIdOptimize;
    private Long recordingIdReference;
    private Long taskIdOptimize;
    private Long taskIdReference;
    private String clipCategory;
    private String aiComparisonResult;
    private String aiModel;
    private String status;
    private LocalDateTime createdAt;

    // Streamer info (populated by service)
    private String anchorNameOptimize;
    private String anchorNameReference;
    private String anchorAvatarOptimize;
    private String anchorAvatarReference;

    // 整场对比时的录制文件名（picker 与列表都要展示，原 recording 删除时回退 cloud_files 快照）
    private String localFileNameOptimize;
    private String localFileNameReference;

    // Clip info (populated by service from AnalysisTask)
    private String clipFilenameOptimize;
    private String clipRemarkOptimize;
    private String clipFilenameReference;
    private String clipRemarkReference;

    public static ComparisonVO fromEntity(Comparison entity) {
        if (entity == null) return null;
        ComparisonVO vo = new ComparisonVO();
        vo.setId(entity.getId());
        vo.setType(entity.getType());
        vo.setRecordingIdOptimize(entity.getRecordingIdOptimize());
        vo.setRecordingIdReference(entity.getRecordingIdReference());
        vo.setTaskIdOptimize(entity.getTaskIdOptimize());
        vo.setTaskIdReference(entity.getTaskIdReference());
        vo.setClipCategory(entity.getClipCategory());
        vo.setAiComparisonResult(entity.getAiComparisonResult());
        vo.setAiModel(entity.getAiModel());
        vo.setStatus(entity.getStatus());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Long getRecordingIdOptimize() {
        return recordingIdOptimize;
    }

    public void setRecordingIdOptimize(Long recordingIdOptimize) {
        this.recordingIdOptimize = recordingIdOptimize;
    }

    public Long getRecordingIdReference() {
        return recordingIdReference;
    }

    public void setRecordingIdReference(Long recordingIdReference) {
        this.recordingIdReference = recordingIdReference;
    }

    public Long getTaskIdOptimize() {
        return taskIdOptimize;
    }

    public void setTaskIdOptimize(Long taskIdOptimize) {
        this.taskIdOptimize = taskIdOptimize;
    }

    public Long getTaskIdReference() {
        return taskIdReference;
    }

    public void setTaskIdReference(Long taskIdReference) {
        this.taskIdReference = taskIdReference;
    }

    public String getClipCategory() {
        return clipCategory;
    }

    public void setClipCategory(String clipCategory) {
        this.clipCategory = clipCategory;
    }

    public String getAiComparisonResult() {
        return aiComparisonResult;
    }

    public void setAiComparisonResult(String aiComparisonResult) {
        this.aiComparisonResult = aiComparisonResult;
    }

    public String getAiModel() {
        return aiModel;
    }

    public void setAiModel(String aiModel) {
        this.aiModel = aiModel;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getAnchorNameOptimize() {
        return anchorNameOptimize;
    }

    public void setAnchorNameOptimize(String anchorNameOptimize) {
        this.anchorNameOptimize = anchorNameOptimize;
    }

    public String getAnchorNameReference() {
        return anchorNameReference;
    }

    public void setAnchorNameReference(String anchorNameReference) {
        this.anchorNameReference = anchorNameReference;
    }

    public String getAnchorAvatarOptimize() {
        return anchorAvatarOptimize;
    }

    public void setAnchorAvatarOptimize(String anchorAvatarOptimize) {
        this.anchorAvatarOptimize = anchorAvatarOptimize;
    }

    public String getAnchorAvatarReference() {
        return anchorAvatarReference;
    }

    public void setAnchorAvatarReference(String anchorAvatarReference) {
        this.anchorAvatarReference = anchorAvatarReference;
    }

    public String getClipFilenameOptimize() {
        return clipFilenameOptimize;
    }

    public void setClipFilenameOptimize(String clipFilenameOptimize) {
        this.clipFilenameOptimize = clipFilenameOptimize;
    }

    public String getClipRemarkOptimize() {
        return clipRemarkOptimize;
    }

    public void setClipRemarkOptimize(String clipRemarkOptimize) {
        this.clipRemarkOptimize = clipRemarkOptimize;
    }

    public String getClipFilenameReference() {
        return clipFilenameReference;
    }

    public void setClipFilenameReference(String clipFilenameReference) {
        this.clipFilenameReference = clipFilenameReference;
    }

    public String getClipRemarkReference() {
        return clipRemarkReference;
    }

    public void setClipRemarkReference(String clipRemarkReference) {
        this.clipRemarkReference = clipRemarkReference;
    }

    public String getLocalFileNameOptimize() {
        return localFileNameOptimize;
    }

    public void setLocalFileNameOptimize(String localFileNameOptimize) {
        this.localFileNameOptimize = localFileNameOptimize;
    }

    public String getLocalFileNameReference() {
        return localFileNameReference;
    }

    public void setLocalFileNameReference(String localFileNameReference) {
        this.localFileNameReference = localFileNameReference;
    }
}
