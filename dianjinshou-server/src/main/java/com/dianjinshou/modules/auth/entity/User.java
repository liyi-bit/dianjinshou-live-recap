package com.dianjinshou.modules.auth.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.dianjinshou.common.entity.BaseEntity;

import java.time.LocalDateTime;

@TableName("users")
public class User extends BaseEntity {

    private String username;
    private String phone;
    private String email;
    private String passwordHash;
    private String avatarUrl;
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
    /** 免费默认密钥下完成的视频 AI 解析次数（v1.0.x 终身累计，v1.1.0 起不再使用但保留审计）。 */
    private Integer defaultAiUsed;

    /** 今日 AI 复盘已消耗次数（v1.1.0 起每日限 10 次；ai_quota_unlimited=1 则不限）。 */
    private Integer dailyAiUsed;

    /** 下一次额度重置时间（本地零点）。读取时若 < NOW，先归零再消耗。 */
    private java.time.LocalDateTime dailyAiResetAt;

    /** 豁免每日限额标记。1 = 无上限；0 = 受每日 10 次限制。 */
    private Integer aiQuotaUnlimited;

    public Integer getDefaultAiUsed() {
        return defaultAiUsed;
    }

    public void setDefaultAiUsed(Integer defaultAiUsed) {
        this.defaultAiUsed = defaultAiUsed;
    }

    public Integer getDailyAiUsed() {
        return dailyAiUsed;
    }

    public void setDailyAiUsed(Integer dailyAiUsed) {
        this.dailyAiUsed = dailyAiUsed;
    }

    public java.time.LocalDateTime getDailyAiResetAt() {
        return dailyAiResetAt;
    }

    public void setDailyAiResetAt(java.time.LocalDateTime dailyAiResetAt) {
        this.dailyAiResetAt = dailyAiResetAt;
    }

    public Integer getAiQuotaUnlimited() {
        return aiQuotaUnlimited;
    }

    public void setAiQuotaUnlimited(Integer aiQuotaUnlimited) {
        this.aiQuotaUnlimited = aiQuotaUnlimited;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Long getOrgId() {
        return orgId;
    }

    public void setOrgId(Long orgId) {
        this.orgId = orgId;
    }

    public Integer getVipLevel() {
        return vipLevel;
    }

    public void setVipLevel(Integer vipLevel) {
        this.vipLevel = vipLevel;
    }

    public LocalDateTime getVipExpireAt() {
        return vipExpireAt;
    }

    public void setVipExpireAt(LocalDateTime vipExpireAt) {
        this.vipExpireAt = vipExpireAt;
    }

    public Long getAiQuotaTotal() {
        return aiQuotaTotal;
    }

    public void setAiQuotaTotal(Long aiQuotaTotal) {
        this.aiQuotaTotal = aiQuotaTotal;
    }

    public Long getAiQuotaUsed() {
        return aiQuotaUsed;
    }

    public void setAiQuotaUsed(Long aiQuotaUsed) {
        this.aiQuotaUsed = aiQuotaUsed;
    }

    public Long getDurationQuotaTotal() {
        return durationQuotaTotal;
    }

    public void setDurationQuotaTotal(Long durationQuotaTotal) {
        this.durationQuotaTotal = durationQuotaTotal;
    }

    public Long getDurationQuotaUsed() {
        return durationQuotaUsed;
    }

    public void setDurationQuotaUsed(Long durationQuotaUsed) {
        this.durationQuotaUsed = durationQuotaUsed;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public LocalDateTime getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(LocalDateTime lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    public String getWechatOpenId() {
        return wechatOpenId;
    }

    public void setWechatOpenId(String wechatOpenId) {
        this.wechatOpenId = wechatOpenId;
    }

    public String getQqOpenId() {
        return qqOpenId;
    }

    public void setQqOpenId(String qqOpenId) {
        this.qqOpenId = qqOpenId;
    }
}
