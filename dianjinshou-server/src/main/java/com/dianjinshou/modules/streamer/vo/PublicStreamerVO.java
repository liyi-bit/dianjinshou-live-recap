package com.dianjinshou.modules.streamer.vo;

import com.dianjinshou.modules.streamer.entity.Streamer;

import java.time.LocalDateTime;

/**
 * 公开视图 — 裁剪掉 orgId / userId / secUid 等内部字段，仅保留对外可见的主播信息。
 * 用于未登录游客访问 /api/v1/public/streamers。
 */
public class PublicStreamerVO {

    private Long id;
    private String platform;
    private String accountId;
    private String anchorName;
    private String anchorAvatar;
    private String accountType;
    private Integer totalSessions;
    private Integer todaySessions;
    private LocalDateTime lastLiveAt;

    public static PublicStreamerVO fromEntity(Streamer s) {
        PublicStreamerVO vo = new PublicStreamerVO();
        vo.id = s.getId();
        vo.platform = s.getPlatform() != null ? s.getPlatform().getCode() : null;
        vo.accountId = s.getAccountId();
        vo.anchorName = s.getAnchorName();
        vo.anchorAvatar = s.getAnchorAvatar();
        vo.accountType = s.getAccountType() != null ? s.getAccountType().getCode() : null;
        vo.totalSessions = s.getTotalSessions();
        vo.todaySessions = s.getTodaySessions();
        vo.lastLiveAt = s.getLastLiveAt();
        return vo;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }
    public String getAccountId() { return accountId; }
    public void setAccountId(String accountId) { this.accountId = accountId; }
    public String getAnchorName() { return anchorName; }
    public void setAnchorName(String anchorName) { this.anchorName = anchorName; }
    public String getAnchorAvatar() { return anchorAvatar; }
    public void setAnchorAvatar(String anchorAvatar) { this.anchorAvatar = anchorAvatar; }
    public String getAccountType() { return accountType; }
    public void setAccountType(String accountType) { this.accountType = accountType; }
    public Integer getTotalSessions() { return totalSessions; }
    public void setTotalSessions(Integer totalSessions) { this.totalSessions = totalSessions; }
    public Integer getTodaySessions() { return todaySessions; }
    public void setTodaySessions(Integer todaySessions) { this.todaySessions = todaySessions; }
    public LocalDateTime getLastLiveAt() { return lastLiveAt; }
    public void setLastLiveAt(LocalDateTime lastLiveAt) { this.lastLiveAt = lastLiveAt; }
}
