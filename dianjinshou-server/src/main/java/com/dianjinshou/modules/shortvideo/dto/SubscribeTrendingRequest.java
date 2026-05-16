package com.dianjinshou.modules.shortvideo.dto;

public class SubscribeTrendingRequest {

    private String platform;
    private String industry;
    private Long minPlayCount;
    private Long minLikeCount;
    private String keywords;

    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }

    public String getIndustry() { return industry; }
    public void setIndustry(String industry) { this.industry = industry; }

    public Long getMinPlayCount() { return minPlayCount; }
    public void setMinPlayCount(Long minPlayCount) { this.minPlayCount = minPlayCount; }

    public Long getMinLikeCount() { return minLikeCount; }
    public void setMinLikeCount(Long minLikeCount) { this.minLikeCount = minLikeCount; }

    public String getKeywords() { return keywords; }
    public void setKeywords(String keywords) { this.keywords = keywords; }
}
