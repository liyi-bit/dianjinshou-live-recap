package com.dianjinshou.modules.vip.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("vip_plans")
public class VipPlan {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;
    private Integer level;
    private Integer durationDays;
    private BigDecimal price;
    private Long aiQuota;
    private Integer maxRooms;
    private Integer maxMembers;
    private String features;
    private Integer isActive;
    private Integer sortOrder;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Integer getLevel() { return level; }
    public void setLevel(Integer level) { this.level = level; }

    public Integer getDurationDays() { return durationDays; }
    public void setDurationDays(Integer durationDays) { this.durationDays = durationDays; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public Long getAiQuota() { return aiQuota; }
    public void setAiQuota(Long aiQuota) { this.aiQuota = aiQuota; }

    public Integer getMaxRooms() { return maxRooms; }
    public void setMaxRooms(Integer maxRooms) { this.maxRooms = maxRooms; }

    public Integer getMaxMembers() { return maxMembers; }
    public void setMaxMembers(Integer maxMembers) { this.maxMembers = maxMembers; }

    public String getFeatures() { return features; }
    public void setFeatures(String features) { this.features = features; }

    public Integer getIsActive() { return isActive; }
    public void setIsActive(Integer isActive) { this.isActive = isActive; }

    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }

    public Integer getDeleted() { return deleted; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
