package com.dianjinshou.modules.admin.vo;

import com.dianjinshou.modules.auth.entity.User;

import java.time.LocalDateTime;

public class AdminUserVO {

    private Long id;
    private String username;
    private String phone;
    private String role;
    private Long orgId;
    private Integer vipLevel;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;

    private Long streamerCount;
    private Long recordingCount;
    private Long todayStreamerCount;
    private Long todayRecordingCount;
    private Integer dailyAiUsed;
    private Integer dailyAiLimit;
    private Boolean dailyAiUnlimited;

    public static AdminUserVO fromEntity(User user) {
        AdminUserVO vo = new AdminUserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setPhone(user.getPhone());
        vo.setRole(user.getRole());
        vo.setOrgId(user.getOrgId());
        vo.setVipLevel(user.getVipLevel());
        vo.setStatus(user.getStatus());
        vo.setCreatedAt(user.getCreatedAt());
        vo.setLastLoginAt(user.getLastLoginAt());
        vo.setDailyAiUsed(user.getDailyAiUsed() != null ? user.getDailyAiUsed() : 0);
        vo.setDailyAiLimit(com.dianjinshou.modules.admin.service.DailyAiQuotaService.DAILY_LIMIT);
        vo.setDailyAiUnlimited(user.getAiQuotaUnlimited() != null && user.getAiQuotaUnlimited() == 1);
        return vo;
    }

    public Long getStreamerCount() { return streamerCount; }
    public void setStreamerCount(Long streamerCount) { this.streamerCount = streamerCount; }
    public Long getRecordingCount() { return recordingCount; }
    public void setRecordingCount(Long recordingCount) { this.recordingCount = recordingCount; }
    public Long getTodayStreamerCount() { return todayStreamerCount; }
    public void setTodayStreamerCount(Long todayStreamerCount) { this.todayStreamerCount = todayStreamerCount; }
    public Long getTodayRecordingCount() { return todayRecordingCount; }
    public void setTodayRecordingCount(Long todayRecordingCount) { this.todayRecordingCount = todayRecordingCount; }
    public Integer getDailyAiUsed() { return dailyAiUsed; }
    public void setDailyAiUsed(Integer dailyAiUsed) { this.dailyAiUsed = dailyAiUsed; }
    public Integer getDailyAiLimit() { return dailyAiLimit; }
    public void setDailyAiLimit(Integer dailyAiLimit) { this.dailyAiLimit = dailyAiLimit; }
    public Boolean getDailyAiUnlimited() { return dailyAiUnlimited; }
    public void setDailyAiUnlimited(Boolean dailyAiUnlimited) { this.dailyAiUnlimited = dailyAiUnlimited; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public Long getOrgId() { return orgId; }
    public void setOrgId(Long orgId) { this.orgId = orgId; }

    public Integer getVipLevel() { return vipLevel; }
    public void setVipLevel(Integer vipLevel) { this.vipLevel = vipLevel; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(LocalDateTime lastLoginAt) { this.lastLoginAt = lastLoginAt; }
}
