package com.dianjinshou.modules.streamer.dto;

public class StreamerQueryRequest {

    private String keyword;
    private String accountType;
    private String platform;
    private Integer isMonitoring;
    private Integer page;
    private Integer size;

    public Integer getIsMonitoring() { return isMonitoring; }
    public void setIsMonitoring(Integer isMonitoring) { this.isMonitoring = isMonitoring; }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public String getAccountType() {
        return accountType;
    }

    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public int getPageOrDefault() {
        return page != null && page > 0 ? page : 1;
    }

    public int getSizeOrDefault() {
        return size != null && size > 0 ? size : 10;
    }
}
