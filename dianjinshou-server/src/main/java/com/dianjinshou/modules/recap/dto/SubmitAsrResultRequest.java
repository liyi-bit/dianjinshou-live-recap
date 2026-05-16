package com.dianjinshou.modules.recap.dto;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.NotEmpty;
import java.util.List;

public class SubmitAsrResultRequest {

    @NotNull(message = "录制ID不能为空")
    private Long recordingId;

    private String industry;
    private String aiModel;

    /**
     * 是否提交后立刻触发 AI 分析。
     * - null 或 true：维持 v1.0.x 老行为（ASR→直接进 AI_PROCESSING + 发 MQ），老客户端兼容
     * - false：仅写入段落，task 状态置 transcribed，等用户在详情页手动点 AI 复盘
     */
    private Boolean autoAnalyze;

    @NotEmpty(message = "ASR结果不能为空")
    private List<AsrSegment> segments;

    public static class AsrSegment {
        private int segmentIndex;
        private String startTime;  // "HH:mm:ss"
        private String endTime;
        private String text;

        public int getSegmentIndex() { return segmentIndex; }
        public void setSegmentIndex(int segmentIndex) { this.segmentIndex = segmentIndex; }
        public String getStartTime() { return startTime; }
        public void setStartTime(String startTime) { this.startTime = startTime; }
        public String getEndTime() { return endTime; }
        public void setEndTime(String endTime) { this.endTime = endTime; }
        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
    }

    public Long getRecordingId() { return recordingId; }
    public void setRecordingId(Long recordingId) { this.recordingId = recordingId; }
    public String getIndustry() { return industry; }
    public void setIndustry(String industry) { this.industry = industry; }
    public String getAiModel() { return aiModel; }
    public void setAiModel(String aiModel) { this.aiModel = aiModel; }
    public Boolean getAutoAnalyze() { return autoAnalyze; }
    public void setAutoAnalyze(Boolean autoAnalyze) { this.autoAnalyze = autoAnalyze; }
    public List<AsrSegment> getSegments() { return segments; }
    public void setSegments(List<AsrSegment> segments) { this.segments = segments; }
}
