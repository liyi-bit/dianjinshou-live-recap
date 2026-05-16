package com.dianjinshou.modules.recap.dto;

import javax.validation.constraints.NotNull;

public class CreateCompetitorReportRequest {

    @NotNull(message = "主播ID不能为空")
    private Long streamerId;

    @NotNull(message = "竞品主播ID不能为空")
    private Long competitorStreamerId;

    private Long recordingId;
    private Long competitorRecordingId;

    public Long getStreamerId() { return streamerId; }
    public void setStreamerId(Long streamerId) { this.streamerId = streamerId; }

    public Long getCompetitorStreamerId() { return competitorStreamerId; }
    public void setCompetitorStreamerId(Long competitorStreamerId) { this.competitorStreamerId = competitorStreamerId; }

    public Long getRecordingId() { return recordingId; }
    public void setRecordingId(Long recordingId) { this.recordingId = recordingId; }

    public Long getCompetitorRecordingId() { return competitorRecordingId; }
    public void setCompetitorRecordingId(Long competitorRecordingId) { this.competitorRecordingId = competitorRecordingId; }
}
