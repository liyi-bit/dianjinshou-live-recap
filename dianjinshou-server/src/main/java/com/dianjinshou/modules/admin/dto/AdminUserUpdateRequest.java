package com.dianjinshou.modules.admin.dto;

public class AdminUserUpdateRequest {

    private String role;
    private Integer status;
    private Integer vipLevel;

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public Integer getVipLevel() { return vipLevel; }
    public void setVipLevel(Integer vipLevel) { this.vipLevel = vipLevel; }
}
