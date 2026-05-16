package com.dianjinshou.modules.adminauth.vo;

public class AdminLoginVO {

    private String accessToken;
    private Long expiresIn;
    private AdminMeVO user;

    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
    public Long getExpiresIn() { return expiresIn; }
    public void setExpiresIn(Long expiresIn) { this.expiresIn = expiresIn; }
    public AdminMeVO getUser() { return user; }
    public void setUser(AdminMeVO user) { this.user = user; }
}
