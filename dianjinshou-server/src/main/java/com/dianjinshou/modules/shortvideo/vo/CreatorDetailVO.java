package com.dianjinshou.modules.shortvideo.vo;

import com.dianjinshou.modules.shortvideo.entity.CreatorVideo;

import java.util.List;

public class CreatorDetailVO extends CreatorVO {

    private List<CreatorVideo> recentVideos;

    public List<CreatorVideo> getRecentVideos() { return recentVideos; }
    public void setRecentVideos(List<CreatorVideo> recentVideos) { this.recentVideos = recentVideos; }
}
