package com.dianjinshou.modules.streamer.vo;

import com.dianjinshou.modules.streamer.entity.Streamer;

import java.time.LocalDateTime;

public class StreamerListVO {

    private Long id;
    private String platform;
    private String accountId;
    private String anchorName;
    private String anchorAvatar;
    private String secUid;
    private String accountType;
    private Long industryId;
    private String industryName;
    private Boolean isMonitoring;
    private Boolean autoAiAnalysis;
    private Boolean cloudSyncEnabled;
    private Integer totalSessions;
    private Integer todaySessions;
    private LocalDateTime lastLiveAt;

    public static StreamerListVO fromEntity(Streamer s) {
        StreamerListVO vo = new StreamerListVO();
        vo.setId(s.getId());
        vo.setPlatform(s.getPlatform() != null ? s.getPlatform().getCode() : null);
        vo.setAccountId(s.getAccountId());
        vo.setAnchorName(s.getAnchorName());
        vo.setAnchorAvatar(s.getAnchorAvatar());
        vo.setSecUid(s.getSecUid());
        vo.setAccountType(s.getAccountType() != null ? s.getAccountType().getCode() : null);
        vo.setIndustryId(s.getIndustryId());
        vo.setIsMonitoring(s.getIsMonitoring());
        vo.setAutoAiAnalysis(s.getAutoAiAnalysis());
        vo.setCloudSyncEnabled(Boolean.TRUE.equals(s.getCloudSyncEnabled()));
        vo.setTotalSessions(s.getTotalSessions());
        vo.setTodaySessions(s.getTodaySessions());
        vo.setLastLiveAt(s.getLastLiveAt());
        return vo;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getAnchorName() {
        return anchorName;
    }

    public void setAnchorName(String anchorName) {
        this.anchorName = anchorName;
    }

    public String getAnchorAvatar() {
        return anchorAvatar;
    }

    public void setAnchorAvatar(String anchorAvatar) {
        this.anchorAvatar = anchorAvatar;
    }

    public String getSecUid() {
        return secUid;
    }

    public void setSecUid(String secUid) {
        this.secUid = secUid;
    }

    public String getAccountType() {
        return accountType;
    }

    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }

    public Long getIndustryId() {
        return industryId;
    }

    public void setIndustryId(Long industryId) {
        this.industryId = industryId;
    }

    public String getIndustryName() {
        return industryName;
    }

    public void setIndustryName(String industryName) {
        this.industryName = industryName;
    }

    public Boolean getIsMonitoring() {
        return isMonitoring;
    }

    public void setIsMonitoring(Boolean isMonitoring) {
        this.isMonitoring = isMonitoring;
    }

    public Boolean getAutoAiAnalysis() {
        return autoAiAnalysis;
    }

    public void setAutoAiAnalysis(Boolean autoAiAnalysis) {
        this.autoAiAnalysis = autoAiAnalysis;
    }

    public Boolean getCloudSyncEnabled() {
        return cloudSyncEnabled;
    }

    public void setCloudSyncEnabled(Boolean cloudSyncEnabled) {
        this.cloudSyncEnabled = cloudSyncEnabled;
    }

    public Integer getTotalSessions() {
        return totalSessions;
    }

    public void setTotalSessions(Integer totalSessions) {
        this.totalSessions = totalSessions;
    }

    public Integer getTodaySessions() {
        return todaySessions;
    }

    public void setTodaySessions(Integer todaySessions) {
        this.todaySessions = todaySessions;
    }

    public LocalDateTime getLastLiveAt() {
        return lastLiveAt;
    }

    public void setLastLiveAt(LocalDateTime lastLiveAt) {
        this.lastLiveAt = lastLiveAt;
    }
}
