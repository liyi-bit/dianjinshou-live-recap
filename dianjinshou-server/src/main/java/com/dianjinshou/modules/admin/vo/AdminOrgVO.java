package com.dianjinshou.modules.admin.vo;

import com.dianjinshou.modules.organization.entity.Organization;

import java.time.LocalDateTime;

public class AdminOrgVO {

    private Long id;
    private String name;
    private Long ownerId;
    private Integer memberCount;
    private Integer vipLevel;
    private LocalDateTime createdAt;

    public static AdminOrgVO fromEntity(Organization org, Long ownerId, int memberCount) {
        AdminOrgVO vo = new AdminOrgVO();
        vo.setId(org.getId());
        vo.setName(org.getName());
        vo.setOwnerId(ownerId);
        vo.setMemberCount(memberCount);
        vo.setVipLevel(org.getVipLevel());
        vo.setCreatedAt(org.getCreatedAt());
        return vo;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }

    public Integer getMemberCount() { return memberCount; }
    public void setMemberCount(Integer memberCount) { this.memberCount = memberCount; }

    public Integer getVipLevel() { return vipLevel; }
    public void setVipLevel(Integer vipLevel) { this.vipLevel = vipLevel; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
