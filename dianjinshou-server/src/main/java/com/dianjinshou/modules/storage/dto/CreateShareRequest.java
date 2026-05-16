package com.dianjinshou.modules.storage.dto;

public class CreateShareRequest {

    private String password;
    private Integer expireHours;
    private Integer maxDownloads;

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public Integer getExpireHours() { return expireHours; }
    public void setExpireHours(Integer expireHours) { this.expireHours = expireHours; }

    public Integer getMaxDownloads() { return maxDownloads; }
    public void setMaxDownloads(Integer maxDownloads) { this.maxDownloads = maxDownloads; }
}
