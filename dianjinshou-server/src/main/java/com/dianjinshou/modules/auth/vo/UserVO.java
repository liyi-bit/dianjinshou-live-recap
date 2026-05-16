package com.dianjinshou.modules.auth.vo;

import com.dianjinshou.modules.auth.entity.User;

import java.time.LocalDateTime;

public class UserVO {

    private Long id;
    private String username;
    private String phone;
    private String avatarUrl;
    private String role;
    private Long orgId;
    private Integer vipLevel;
    private LocalDateTime vipExpireAt;

    public static UserVO fromEntity(User user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setPhone(maskPhone(user.getPhone()));
        vo.setAvatarUrl(user.getAvatarUrl());
        vo.setRole(user.getRole());
        vo.setOrgId(user.getOrgId());
        vo.setVipLevel(user.getVipLevel());
        vo.setVipExpireAt(user.getVipExpireAt());
        return vo;
    }

    private static String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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
}
