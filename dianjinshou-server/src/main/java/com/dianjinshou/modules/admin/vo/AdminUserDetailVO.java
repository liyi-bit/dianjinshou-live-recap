package com.dianjinshou.modules.admin.vo;

import com.dianjinshou.modules.auth.entity.User;

import java.time.LocalDateTime;

public class AdminUserDetailVO {

    private Long id;
    private String username;
    private String phone;
    private String email;
    private String role;
    private Long orgId;
    private Integer vipLevel;
    private LocalDateTime vipExpireAt;
    private Long aiQuotaTotal;
    private Long aiQuotaUsed;
    private Long durationQuotaTotal;
    private Long durationQuotaUsed;
    private Integer status;
    private LocalDateTime lastLoginAt;
    private String wechatOpenId;
    private String qqOpenId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static AdminUserDetailVO fromEntity(User u) {
        AdminUserDetailVO v = new AdminUserDetailVO();
        v.id = u.getId();
        v.username = u.getUsername();
        v.phone = u.getPhone();
        v.email = u.getEmail();
        v.role = u.getRole();
        v.orgId = u.getOrgId();
        v.vipLevel = u.getVipLevel();
        v.vipExpireAt = u.getVipExpireAt();
        v.aiQuotaTotal = u.getAiQuotaTotal();
        v.aiQuotaUsed = u.getAiQuotaUsed();
        v.durationQuotaTotal = u.getDurationQuotaTotal();
        v.durationQuotaUsed = u.getDurationQuotaUsed();
        v.status = u.getStatus();
        v.lastLoginAt = u.getLastLoginAt();
        v.wechatOpenId = u.getWechatOpenId();
        v.qqOpenId = u.getQqOpenId();
        v.createdAt = u.getCreatedAt();
        v.updatedAt = u.getUpdatedAt();
        return v;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public Long getOrgId() { return orgId; }
    public void setOrgId(Long orgId) { this.orgId = orgId; }
    public Integer getVipLevel() { return vipLevel; }
    public void setVipLevel(Integer vipLevel) { this.vipLevel = vipLevel; }
    public LocalDateTime getVipExpireAt() { return vipExpireAt; }
    public void setVipExpireAt(LocalDateTime vipExpireAt) { this.vipExpireAt = vipExpireAt; }
    public Long getAiQuotaTotal() { return aiQuotaTotal; }
    public void setAiQuotaTotal(Long aiQuotaTotal) { this.aiQuotaTotal = aiQuotaTotal; }
    public Long getAiQuotaUsed() { return aiQuotaUsed; }
    public void setAiQuotaUsed(Long aiQuotaUsed) { this.aiQuotaUsed = aiQuotaUsed; }
    public Long getDurationQuotaTotal() { return durationQuotaTotal; }
    public void setDurationQuotaTotal(Long durationQuotaTotal) { this.durationQuotaTotal = durationQuotaTotal; }
    public Long getDurationQuotaUsed() { return durationQuotaUsed; }
    public void setDurationQuotaUsed(Long durationQuotaUsed) { this.durationQuotaUsed = durationQuotaUsed; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public LocalDateTime getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(LocalDateTime lastLoginAt) { this.lastLoginAt = lastLoginAt; }
    public String getWechatOpenId() { return wechatOpenId; }
    public void setWechatOpenId(String wechatOpenId) { this.wechatOpenId = wechatOpenId; }
    public String getQqOpenId() { return qqOpenId; }
    public void setQqOpenId(String qqOpenId) { this.qqOpenId = qqOpenId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
