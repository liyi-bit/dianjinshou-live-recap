package com.dianjinshou.modules.adminauth.vo;

import com.dianjinshou.modules.adminauth.entity.AdminAccount;

import java.time.LocalDateTime;

public class AdminMeVO {

    private Long id;
    private String username;
    private String displayName;
    private String email;
    private String role;
    private Integer status;
    private LocalDateTime lastLoginAt;

    public static AdminMeVO fromEntity(AdminAccount a) {
        AdminMeVO vo = new AdminMeVO();
        vo.id = a.getId();
        vo.username = a.getUsername();
        vo.displayName = a.getDisplayName();
        vo.email = a.getEmail();
        vo.role = a.getRole();
        vo.status = a.getStatus();
        vo.lastLoginAt = a.getLastLoginAt();
        return vo;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public LocalDateTime getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(LocalDateTime lastLoginAt) { this.lastLoginAt = lastLoginAt; }
}
