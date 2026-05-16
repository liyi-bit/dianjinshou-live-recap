package com.dianjinshou.modules.shortvideo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dianjinshou.common.exception.BusinessException;
import com.dianjinshou.common.response.ErrorCode;
import com.dianjinshou.modules.shortvideo.entity.Creator;
import com.dianjinshou.modules.shortvideo.entity.CreatorVideo;
import com.dianjinshou.modules.shortvideo.mapper.CreatorMapper;
import com.dianjinshou.modules.shortvideo.mapper.CreatorVideoMapper;
import com.dianjinshou.modules.shortvideo.provider.CreatorDataProvider;
import com.dianjinshou.modules.shortvideo.vo.CreatorDetailVO;
import com.dianjinshou.modules.shortvideo.vo.CreatorVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CreatorSearchService {

    private static final Logger log = LoggerFactory.getLogger(CreatorSearchService.class);

    private final CreatorMapper creatorMapper;
    private final CreatorVideoMapper creatorVideoMapper;
    private final CreatorDataProvider creatorDataProvider;

    public CreatorSearchService(CreatorMapper creatorMapper,
                                CreatorVideoMapper creatorVideoMapper,
                                CreatorDataProvider creatorDataProvider) {
        this.creatorMapper = creatorMapper;
        this.creatorVideoMapper = creatorVideoMapper;
        this.creatorDataProvider = creatorDataProvider;
    }

    public List<CreatorVO> searchCreators(String keyword, String platform, String industry,
                                          Long minFollowers, Long maxFollowers, int page, int size) {
        // First try local DB
        LambdaQueryWrapper<Creator> query = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            query.like(Creator::getNickname, keyword);
        }
        if (platform != null && !platform.isEmpty()) {
            query.eq(Creator::getPlatform, platform);
        }
        if (industry != null && !industry.isEmpty()) {
            query.eq(Creator::getIndustry, industry);
        }
        if (minFollowers != null) {
            query.ge(Creator::getFollowerCount, minFollowers);
        }
        if (maxFollowers != null) {
            query.le(Creator::getFollowerCount, maxFollowers);
        }
        query.orderByDesc(Creator::getFollowerCount);
        query.last("LIMIT " + size + " OFFSET " + ((page - 1) * size));

        List<Creator> localResults = creatorMapper.selectList(query);

        if (!localResults.isEmpty()) {
            List<CreatorVO> vos = new ArrayList<CreatorVO>();
            for (Creator c : localResults) {
                vos.add(CreatorVO.fromEntity(c));
            }
            return vos;
        }

        // Fallback to external provider
        List<Creator> externalResults = creatorDataProvider.searchCreators(
                keyword, platform, industry, minFollowers, maxFollowers, page, size);

        // Cache external results to local DB
        List<CreatorVO> vos = new ArrayList<CreatorVO>();
        for (Creator c : externalResults) {
            // Check if already exists
            LambdaQueryWrapper<Creator> existCheck = new LambdaQueryWrapper<>();
            existCheck.eq(Creator::getPlatform, c.getPlatform())
                      .eq(Creator::getCreatorId, c.getCreatorId());
            Creator existing = creatorMapper.selectOne(existCheck);
            if (existing == null) {
                creatorMapper.insert(c);
            } else {
                c.setId(existing.getId());
            }
            vos.add(CreatorVO.fromEntity(c));
        }

        log.info("Creator search: keyword={}, results={}", keyword, vos.size());
        return vos;
    }

    public CreatorDetailVO getCreatorDetail(Long id) {
        Creator creator = creatorMapper.selectById(id);
        if (creator == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "达人不存在");
        }

        CreatorDetailVO detail = new CreatorDetailVO();
        detail.setId(creator.getId());
        detail.setPlatform(creator.getPlatform());
        detail.setCreatorId(creator.getCreatorId());
        detail.setNickname(creator.getNickname());
        detail.setAvatarUrl(creator.getAvatarUrl());
        detail.setFollowerCount(creator.getFollowerCount());
        detail.setVideoCount(creator.getVideoCount());
        detail.setIndustry(creator.getIndustry());
        detail.setDescription(creator.getDescription());

        // Get recent videos from DB
        LambdaQueryWrapper<CreatorVideo> videoQuery = new LambdaQueryWrapper<>();
        videoQuery.eq(CreatorVideo::getCreatorId, id)
                  .orderByDesc(CreatorVideo::getPlayCount)
                  .last("LIMIT 30");
        List<CreatorVideo> videos = creatorVideoMapper.selectList(videoQuery);

        // If no videos in DB, try external provider
        if (videos.isEmpty()) {
            videos = creatorDataProvider.getCreatorVideos(creator.getPlatform(), creator.getCreatorId(), 30);
            for (CreatorVideo v : videos) {
                v.setCreatorId(id);
                creatorVideoMapper.insert(v);
            }
        }

        detail.setRecentVideos(videos);
        return detail;
    }
}
