package com.dianjinshou.modules.streamer.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.dianjinshou.common.entity.BaseEntity;
import com.dianjinshou.common.enums.AccountType;
import com.dianjinshou.common.enums.Platform;

import java.time.LocalDateTime;
import java.time.LocalTime;

@TableName("streamers")
public class Streamer extends BaseEntity {

    private Long userId;
    private Long orgId;
    private Platform platform;
    private String roomId;
    private String roomUrl;
    private String anchorName;
    private String anchorAvatar;
    private String secUid;
    private String accountId;
    private Long industryId;
    private AccountType accountType;
    private String liveRoomMode;
    private String accountStage;
    private String accountIssue;
    private String accountLevel;
    private String trafficStructure;
    private LocalTime broadcastTimeStart;
    private LocalTime broadcastTimeEnd;
    private String defaultLanguage;
    private Boolean isMonitoring;
    private Boolean autoAiAnalysis;
    private Boolean cloudSyncEnabled;
    private String monitorConfig;
    private String shipinhaoAuthToken;
    private Integer totalSessions;
    private Integer todaySessions;
    private LocalDateTime lastLiveAt;
    private Integer status;

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

    public Platform getPlatform() {
        return platform;
    }

    public void setPlatform(Platform platform) {
        this.platform = platform;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getRoomUrl() {
        return roomUrl;
    }

    public void setRoomUrl(String roomUrl) {
        this.roomUrl = roomUrl;
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

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public Long getIndustryId() {
        return industryId;
    }

    public void setIndustryId(Long industryId) {
        this.industryId = industryId;
    }

    public AccountType getAccountType() {
        return accountType;
    }

    public void setAccountType(AccountType accountType) {
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

    public String getAccountIssue() {
        return accountIssue;
    }

    public void setAccountIssue(String accountIssue) {
        this.accountIssue = accountIssue;
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

    public String getDefaultLanguage() {
        return defaultLanguage;
    }

    public void setDefaultLanguage(String defaultLanguage) {
        this.defaultLanguage = defaultLanguage;
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

    public String getMonitorConfig() {
        return monitorConfig;
    }

    public void setMonitorConfig(String monitorConfig) {
        this.monitorConfig = monitorConfig;
    }

    public String getShipinhaoAuthToken() {
        return shipinhaoAuthToken;
    }

    public void setShipinhaoAuthToken(String shipinhaoAuthToken) {
        this.shipinhaoAuthToken = shipinhaoAuthToken;
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

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
