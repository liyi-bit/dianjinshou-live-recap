package com.dianjinshou.modules.shortvideo.vo;

import com.dianjinshou.modules.shortvideo.entity.Creator;

public class CreatorVO {

    private Long id;
    private String platform;
    private String creatorId;
    private String nickname;
    private String avatarUrl;
    private Long followerCount;
    private Integer videoCount;
    private String industry;
    private String description;

    public static CreatorVO fromEntity(Creator entity) {
        CreatorVO vo = new CreatorVO();
        vo.setId(entity.getId());
        vo.setPlatform(entity.getPlatform());
        vo.setCreatorId(entity.getCreatorId());
        vo.setNickname(entity.getNickname());
        vo.setAvatarUrl(entity.getAvatarUrl());
        vo.setFollowerCount(entity.getFollowerCount());
        vo.setVideoCount(entity.getVideoCount());
        vo.setIndustry(entity.getIndustry());
        vo.setDescription(entity.getDescription());
        return vo;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }

    public String getCreatorId() { return creatorId; }
    public void setCreatorId(String creatorId) { this.creatorId = creatorId; }

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public Long getFollowerCount() { return followerCount; }
    public void setFollowerCount(Long followerCount) { this.followerCount = followerCount; }

    public Integer getVideoCount() { return videoCount; }
    public void setVideoCount(Integer videoCount) { this.videoCount = videoCount; }

    public String getIndustry() { return industry; }
    public void setIndustry(String industry) { this.industry = industry; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
