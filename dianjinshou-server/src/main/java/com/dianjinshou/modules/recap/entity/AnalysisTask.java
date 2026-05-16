package com.dianjinshou.modules.recap.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.dianjinshou.common.entity.BaseEntity;

import java.time.LocalDateTime;

@TableName("analysis_tasks")
public class AnalysisTask extends BaseEntity {

    private Long recordingId;
    private Long userId;
    private Long orgId;
    private String type;
    private Long parentTaskId;
    private Integer clipStart;
    private Integer clipEnd;
    private String clipCategory;
    private String clipFilename;
    private String clipFilePath;
    private String clipRemark;
    private String status;
    private Integer priority;
    private String aiModel;
    private String industry;
    private String asrText;
    private Integer asrWordCount;
    private String aiResult;
    private String aiDiagnosis;
    private String keywordSummary;
    private String sensitiveWords;
    private Integer sensitiveCount;
    private String contentCompass;
    private String optimizedText;
    private String optimizationAction;
    private String optimizationGoal;
    private String summary;
    private Long consumedChars;
    private String errorMsg;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;

    public Long getRecordingId() {
        return recordingId;
    }

    public void setRecordingId(Long recordingId) {
        this.recordingId = recordingId;
    }

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

    public Long getParentTaskId() {
        return parentTaskId;
    }

    public void setParentTaskId(Long parentTaskId) {
        this.parentTaskId = parentTaskId;
    }

    public Integer getClipStart() {
        return clipStart;
    }

    public void setClipStart(Integer clipStart) {
        this.clipStart = clipStart;
    }

    public Integer getClipEnd() {
        return clipEnd;
    }

    public void setClipEnd(Integer clipEnd) {
        this.clipEnd = clipEnd;
    }

    public String getClipCategory() {
        return clipCategory;
    }

    public void setClipCategory(String clipCategory) {
        this.clipCategory = clipCategory;
    }

    public String getClipFilename() {
        return clipFilename;
    }

    public void setClipFilename(String clipFilename) {
        this.clipFilename = clipFilename;
    }

    public String getClipFilePath() {
        return clipFilePath;
    }

    public void setClipFilePath(String clipFilePath) {
        this.clipFilePath = clipFilePath;
    }

    public String getClipRemark() {
        return clipRemark;
    }

    public void setClipRemark(String clipRemark) {
        this.clipRemark = clipRemark;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public String getAiModel() {
        return aiModel;
    }

    public void setAiModel(String aiModel) {
        this.aiModel = aiModel;
    }

    public String getIndustry() {
        return industry;
    }

    public void setIndustry(String industry) {
        this.industry = industry;
    }

    public String getAsrText() {
        return asrText;
    }

    public void setAsrText(String asrText) {
        this.asrText = asrText;
    }

    public Integer getAsrWordCount() {
        return asrWordCount;
    }

    public void setAsrWordCount(Integer asrWordCount) {
        this.asrWordCount = asrWordCount;
    }

    public String getAiResult() {
        return aiResult;
    }

    public void setAiResult(String aiResult) {
        this.aiResult = aiResult;
    }

    public String getAiDiagnosis() {
        return aiDiagnosis;
    }

    public void setAiDiagnosis(String aiDiagnosis) {
        this.aiDiagnosis = aiDiagnosis;
    }

    public String getKeywordSummary() {
        return keywordSummary;
    }

    public void setKeywordSummary(String keywordSummary) {
        this.keywordSummary = keywordSummary;
    }

    public String getSensitiveWords() {
        return sensitiveWords;
    }

    public void setSensitiveWords(String sensitiveWords) {
        this.sensitiveWords = sensitiveWords;
    }

    public Integer getSensitiveCount() {
        return sensitiveCount;
    }

    public void setSensitiveCount(Integer sensitiveCount) {
        this.sensitiveCount = sensitiveCount;
    }

    public String getContentCompass() {
        return contentCompass;
    }

    public void setContentCompass(String contentCompass) {
        this.contentCompass = contentCompass;
    }

    public String getOptimizedText() {
        return optimizedText;
    }

    public void setOptimizedText(String optimizedText) {
        this.optimizedText = optimizedText;
    }

    public String getOptimizationAction() {
        return optimizationAction;
    }

    public void setOptimizationAction(String optimizationAction) {
        this.optimizationAction = optimizationAction;
    }

    public String getOptimizationGoal() {
        return optimizationGoal;
    }

    public void setOptimizationGoal(String optimizationGoal) {
        this.optimizationGoal = optimizationGoal;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public Long getConsumedChars() {
        return consumedChars;
    }

    public void setConsumedChars(Long consumedChars) {
        this.consumedChars = consumedChars;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }
}
