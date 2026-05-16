package com.dianjinshou.modules.streamer.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.time.LocalTime;

public class CreateStreamerRequest {

    @NotBlank(message = "平台不能为空")
    private String platform;

    private String accountId;

    private String anchorName;

    private String anchorAvatar;

    private String secUid;

    private Long industryId;

    private String accountType;

    private String liveRoomMode;

    private String accountStage;

    private String accountLevel;

    private String trafficStructure;

    private LocalTime broadcastTimeStart;

    private LocalTime broadcastTimeEnd;

    @Size(max = 200, message = "账号问题描述不能超过200字")
    private String accountIssue;

    private String defaultLanguage;

    private Boolean cloudSyncEnabled;

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

    public Long getIndustryId() {
        return industryId;
    }

    public void setIndustryId(Long industryId) {
        this.industryId = industryId;
    }

    public String getAccountType() {
        return accountType;
    }

    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }

    public String getLiveRoomMode() {
        return liveRoomMode;
    }

    public void setLiveRoomMode(String liveRoomMode) {
        this.liveRoomMode = liveRoomMode;
    }

    public String getAccountStage() {
        return accountStage;
    }

    public void setAccountStage(String accountStage) {
        this.accountStage = accountStage;
    }

    public String getAccountLevel() {
        return accountLevel;
    }

    public void setAccountLevel(String accountLevel) {
        this.accountLevel = accountLevel;
    }

    public String getTrafficStructure() {
        return trafficStructure;
    }

    public void setTrafficStructure(String trafficStructure) {
        this.trafficStructure = trafficStructure;
    }

    public LocalTime getBroadcastTimeStart() {
        return broadcastTimeStart;
    }

    public void setBroadcastTimeStart(LocalTime broadcastTimeStart) {
        this.broadcastTimeStart = broadcastTimeStart;
    }

    public LocalTime getBroadcastTimeEnd() {
        return broadcastTimeEnd;
    }

    public void setBroadcastTimeEnd(LocalTime broadcastTimeEnd) {
        this.broadcastTimeEnd = broadcastTimeEnd;
    }

    public String getAccountIssue() {
        return accountIssue;
    }

    public void setAccountIssue(String accountIssue) {
        this.accountIssue = accountIssue;
    }

    public String getDefaultLanguage() {
        return defaultLanguage;
    }

    public void setDefaultLanguage(String defaultLanguage) {
        this.defaultLanguage = defaultLanguage;
    }

    public Boolean getCloudSyncEnabled() {
        return cloudSyncEnabled;
    }

    public void setCloudSyncEnabled(Boolean cloudSyncEnabled) {
        this.cloudSyncEnabled = cloudSyncEnabled;
    }
}
