package com.dianjinshou.modules.shortvideo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("trending_subscriptions")
public class TrendingSubscription {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long orgId;
    private String platform;
    private String industry;
    private Long minPlayCount;
    private Long minLikeCount;
    private String keywords;
    private Integer notifyEnabled;
    private LocalDateTime createdAt;
    @TableLogic
    private Integer deleted;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getOrgId() { return orgId; }
    public void setOrgId(Long orgId) { this.orgId = orgId; }

    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }

    public String getIndustry() { return industry; }
    public void setIndustry(String industry) { this.industry = industry; }

    public Long getMinPlayCount() { return minPlayCount; }
    public void setMinPlayCount(Long minPlayCount) { this.minPlayCount = minPlayCount; }

    public Long getMinLikeCount() { return minLikeCount; }
    public void setMinLikeCount(Long minLikeCount) { this.minLikeCount = minLikeCount; }

    public String getKeywords() { return keywords; }
    public void setKeywords(String keywords) { this.keywords = keywords; }

    public Integer getNotifyEnabled() { return notifyEnabled; }
    public void setNotifyEnabled(Integer notifyEnabled) { this.notifyEnabled = notifyEnabled; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public Integer getDeleted() { return deleted; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
