package com.dianjinshou.modules.recap.vo;

import com.dianjinshou.modules.recap.entity.AnalysisTask;

import java.time.LocalDateTime;

public class AnalysisTaskVO {

    private Long id;
    private Long recordingId;
    private String type;
    private String status;
    private String aiModel;
    private String industry;
    private Integer clipStart;
    private Integer clipEnd;
    private String clipCategory;
    private String clipFilename;
    private String clipFilePath;
    private String clipRemark;
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
    private LocalDateTime createdAt;

    public static AnalysisTaskVO fromEntity(AnalysisTask task) {
        AnalysisTaskVO vo = new AnalysisTaskVO();
        vo.setId(task.getId());
        vo.setRecordingId(task.getRecordingId());
        vo.setType(task.getType());
        vo.setStatus(task.getStatus());
        vo.setAiModel(task.getAiModel());
        vo.setIndustry(task.getIndustry());
        vo.setClipStart(task.getClipStart());
        vo.setClipEnd(task.getClipEnd());
        vo.setClipCategory(task.getClipCategory());
        vo.setClipFilename(task.getClipFilename());
        vo.setClipFilePath(task.getClipFilePath());
        vo.setClipRemark(task.getClipRemark());
        vo.setAsrWordCount(task.getAsrWordCount());
        vo.setAiResult(task.getAiResult());
        vo.setAiDiagnosis(task.getAiDiagnosis());
        vo.setKeywordSummary(task.getKeywordSummary());
        vo.setSensitiveWords(task.getSensitiveWords());
        vo.setSensitiveCount(task.getSensitiveCount());
        vo.setContentCompass(task.getContentCompass());
        vo.setOptimizedText(task.getOptimizedText());
        vo.setOptimizationAction(task.getOptimizationAction());
        vo.setOptimizationGoal(task.getOptimizationGoal());
        vo.setSummary(task.getSummary());
        vo.setConsumedChars(task.getConsumedChars());
        vo.setErrorMsg(task.getErrorMsg());
        vo.setStartedAt(task.getStartedAt());
        vo.setCompletedAt(task.getCompletedAt());
        vo.setCreatedAt(task.getCreatedAt());
        return vo;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRecordingId() {
        return recordingId;
    }

    public void setRecordingId(Long recordingId) {
        this.recordingId = recordingId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
