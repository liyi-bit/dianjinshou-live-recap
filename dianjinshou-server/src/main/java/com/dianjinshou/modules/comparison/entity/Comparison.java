package com.dianjinshou.modules.comparison.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.dianjinshou.common.entity.BaseEntity;

@TableName("comparisons")
public class Comparison extends BaseEntity {

    private Long userId;
    private Long orgId;
    private String type;
    private Long recordingIdOptimize;
    private Long recordingIdReference;
    private Long taskIdOptimize;
    private Long taskIdReference;
    private String clipCategory;
    private String aiComparisonResult;
    private String aiModel;
    private String status;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getOrgId() {
        return orgId;
    }

    public void setOrgId(Long orgId) {
        this.orgId = orgId;
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
}
