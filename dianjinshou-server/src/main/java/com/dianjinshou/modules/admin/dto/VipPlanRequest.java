package com.dianjinshou.modules.admin.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

public class VipPlanRequest {

    @NotBlank(message = "套餐名称不能为空")
    private String name;

    @NotNull(message = "等级不能为空")
    private Integer level;

    @NotNull(message = "价格不能为空")
    private BigDecimal price;

    @NotNull(message = "时长不能为空")
    private Integer durationDays;

    private Long aiQuota;
    private Integer maxRooms;
    private Integer maxMembers;
    private String features;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Integer getLevel() { return level; }
    public void setLevel(Integer level) { this.level = level; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public Integer getDurationDays() { return durationDays; }
    public void setDurationDays(Integer durationDays) { this.durationDays = durationDays; }

    public Long getAiQuota() { return aiQuota; }
    public void setAiQuota(Long aiQuota) { this.aiQuota = aiQuota; }

    public Integer getMaxRooms() { return maxRooms; }
    public void setMaxRooms(Integer maxRooms) { this.maxRooms = maxRooms; }

    public Integer getMaxMembers() { return maxMembers; }
    public void setMaxMembers(Integer maxMembers) { this.maxMembers = maxMembers; }

    public String getFeatures() { return features; }
    public void setFeatures(String features) { this.features = features; }
}
