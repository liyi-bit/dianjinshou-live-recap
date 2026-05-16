package com.dianjinshou.modules.recap.vo;

import com.dianjinshou.modules.recap.entity.CompetitorReport;

import java.time.LocalDateTime;

public class CompetitorReportVO {

    private Long id;
    private Long streamerId;
    private Long competitorStreamerId;
    private String streamerName;
    private String competitorStreamerName;
    private String streamerAvatar;
    private String competitorStreamerAvatar;
    private String report;
    private String aiModel;
    private String status;
    private LocalDateTime createdAt;

    public static CompetitorReportVO fromEntity(CompetitorReport entity,
                                                  String streamerName,
                                                  String competitorStreamerName,
                                                  String streamerAvatar,
                                                  String competitorStreamerAvatar) {
        CompetitorReportVO vo = new CompetitorReportVO();
        vo.setId(entity.getId());
        vo.setStreamerId(entity.getStreamerId());
        vo.setCompetitorStreamerId(entity.getCompetitorStreamerId());
        vo.setStreamerName(streamerName);
        vo.setCompetitorStreamerName(competitorStreamerName);
        vo.setStreamerAvatar(streamerAvatar);
        vo.setCompetitorStreamerAvatar(competitorStreamerAvatar);
        vo.setReport(entity.getReport());
        vo.setAiModel(entity.getAiModel());
        vo.setStatus(entity.getStatus());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getStreamerId() { return streamerId; }
    public void setStreamerId(Long streamerId) { this.streamerId = streamerId; }

    public Long getCompetitorStreamerId() { return competitorStreamerId; }
    public void setCompetitorStreamerId(Long competitorStreamerId) { this.competitorStreamerId = competitorStreamerId; }

    public String getStreamerName() { return streamerName; }
    public void setStreamerName(String streamerName) { this.streamerName = streamerName; }

    public String getCompetitorStreamerName() { return competitorStreamerName; }
    public void setCompetitorStreamerName(String competitorStreamerName) { this.competitorStreamerName = competitorStreamerName; }

    public String getStreamerAvatar() { return streamerAvatar; }
    public void setStreamerAvatar(String streamerAvatar) { this.streamerAvatar = streamerAvatar; }

    public String getCompetitorStreamerAvatar() { return competitorStreamerAvatar; }
    public void setCompetitorStreamerAvatar(String competitorStreamerAvatar) { this.competitorStreamerAvatar = competitorStreamerAvatar; }

    public String getReport() { return report; }
    public void setReport(String report) { this.report = report; }

    public String getAiModel() { return aiModel; }
    public void setAiModel(String aiModel) { this.aiModel = aiModel; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
