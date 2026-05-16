package com.dianjinshou.modules.shortvideo.provider;

import com.dianjinshou.modules.shortvideo.entity.Creator;
import com.dianjinshou.modules.shortvideo.entity.CreatorVideo;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class MockCreatorDataProvider implements CreatorDataProvider {

    @Override
    public List<Creator> searchCreators(String keyword, String platform, String industry,
                                         Long minFollowers, Long maxFollowers, int page, int size) {
        List<Creator> results = new ArrayList<Creator>();
        // Return mock data for development
        Creator c = new Creator();
        c.setPlatform(platform != null ? platform : "douyin");
        c.setCreatorId("mock_" + System.currentTimeMillis());
        c.setNickname(keyword != null ? keyword + "达人" : "模拟达人");
        c.setFollowerCount(500000L);
        c.setVideoCount(120);
        c.setIndustry(industry != null ? industry : "美妆");
        c.setDescription("模拟达人数据，仅供开发测试");
        results.add(c);
        return results;
    }

    @Override
    public List<CreatorVideo> getCreatorVideos(String platform, String creatorId, int limit) {
        List<CreatorVideo> results = new ArrayList<CreatorVideo>();
        CreatorVideo v = new CreatorVideo();
        v.setVideoId("mock_video_1");
        v.setTitle("模拟爆款视频");
        v.setPlayCount(1000000L);
        v.setLikeCount(50000L);
        v.setCommentCount(3000L);
        v.setShareCount(1200L);
        v.setPublishTime(LocalDateTime.now().minusDays(3));
        v.setDuration(30);
        results.add(v);
        return results;
    }
}
