package com.dianjinshou.modules.organization.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.dianjinshou.common.entity.BaseEntity;

import java.time.LocalDateTime;

@TableName("organizations")
public class Organization extends BaseEntity {

    private Long ownerId;
    private String name;
    private Integer maxMembers;
    private Integer vipLevel;
    private LocalDateTime vipExpireAt;

    public Long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getMaxMembers() {
        return maxMembers;
    }

    public void setMaxMembers(Integer maxMembers) {
        this.maxMembers = maxMembers;
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
}
