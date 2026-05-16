package com.dianjinshou.modules.shortvideo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dianjinshou.common.exception.BusinessException;
import com.dianjinshou.common.response.ErrorCode;
import com.dianjinshou.common.security.SecurityContextHelper;
import com.dianjinshou.modules.shortvideo.dto.SubscribeCreatorRequest;
import com.dianjinshou.modules.shortvideo.dto.SubscribeTrendingRequest;
import com.dianjinshou.modules.shortvideo.entity.CreatorSubscription;
import com.dianjinshou.modules.shortvideo.entity.TrendingAlert;
import com.dianjinshou.modules.shortvideo.entity.TrendingSubscription;
import com.dianjinshou.modules.shortvideo.mapper.CreatorSubscriptionMapper;
import com.dianjinshou.modules.shortvideo.mapper.TrendingAlertMapper;
import com.dianjinshou.modules.shortvideo.mapper.TrendingSubscriptionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SubscriptionService {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionService.class);
    private static final int MAX_CREATOR_SUBSCRIPTIONS = 50;
    private static final int MAX_TRENDING_RULES = 10;
    private static final long MIN_PLAY_COUNT_THRESHOLD = 100000L;

    private final CreatorSubscriptionMapper creatorSubscriptionMapper;
    private final TrendingSubscriptionMapper trendingSubscriptionMapper;
    private final TrendingAlertMapper trendingAlertMapper;

    public SubscriptionService(CreatorSubscriptionMapper creatorSubscriptionMapper,
                               TrendingSubscriptionMapper trendingSubscriptionMapper,
                               TrendingAlertMapper trendingAlertMapper) {
        this.creatorSubscriptionMapper = creatorSubscriptionMapper;
        this.trendingSubscriptionMapper = trendingSubscriptionMapper;
        this.trendingAlertMapper = trendingAlertMapper;
    }

    // ========== Creator Subscriptions ==========

    public CreatorSubscription subscribeCreator(SubscribeCreatorRequest request) {
        Long userId = SecurityContextHelper.currentUserId();
        Long orgId = SecurityContextHelper.currentOrgId();
        if (userId == null || orgId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        // Check limit
        long count = creatorSubscriptionMapper.selectCount(
                new LambdaQueryWrapper<CreatorSubscription>().eq(CreatorSubscription::getUserId, userId));
        if (count >= MAX_CREATOR_SUBSCRIPTIONS) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "达人订阅数量已达上限(" + MAX_CREATOR_SUBSCRIPTIONS + ")");
        }

        // Check duplicate
        CreatorSubscription existing = creatorSubscriptionMapper.selectOne(
                new LambdaQueryWrapper<CreatorSubscription>()
                        .eq(CreatorSubscription::getUserId, userId)
                        .eq(CreatorSubscription::getCreatorId, request.getCreatorId()));
        if (existing != null) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "已订阅该达人");
        }

        CreatorSubscription sub = new CreatorSubscription();
        sub.setUserId(userId);
        sub.setOrgId(orgId);
        sub.setCreatorId(request.getCreatorId());
        sub.setNotifyOnNewVideo(request.getNotifyOnNewVideo() != null && request.getNotifyOnNewVideo() ? 1 : 1);
        sub.setDeleted(0);
        creatorSubscriptionMapper.insert(sub);

        log.info("Creator subscribed: userId={}, creatorId={}", userId, request.getCreatorId());
        return sub;
    }

    public List<CreatorSubscription> listCreatorSubscriptions() {
        Long userId = SecurityContextHelper.currentUserId();
        return creatorSubscriptionMapper.selectList(
                new LambdaQueryWrapper<CreatorSubscription>()
                        .eq(CreatorSubscription::getUserId, userId)
                        .orderByDesc(CreatorSubscription::getCreatedAt));
    }

    // ========== Trending Subscriptions ==========

    public TrendingSubscription subscribeTrending(SubscribeTrendingRequest request) {
        Long userId = SecurityContextHelper.currentUserId();
        Long orgId = SecurityContextHelper.currentOrgId();
        if (userId == null || orgId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        long count = trendingSubscriptionMapper.selectCount(
                new LambdaQueryWrapper<TrendingSubscription>().eq(TrendingSubscription::getUserId, userId));
        if (count >= MAX_TRENDING_RULES) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "爆款订阅规则已达上限(" + MAX_TRENDING_RULES + ")");
        }

        if (request.getMinPlayCount() != null && request.getMinPlayCount() < MIN_PLAY_COUNT_THRESHOLD) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "最低播放量不能低于10万");
        }

        TrendingSubscription sub = new TrendingSubscription();
        sub.setUserId(userId);
        sub.setOrgId(orgId);
        sub.setPlatform(request.getPlatform());
        sub.setIndustry(request.getIndustry());
        sub.setMinPlayCount(request.getMinPlayCount() != null ? request.getMinPlayCount() : MIN_PLAY_COUNT_THRESHOLD);
        sub.setMinLikeCount(request.getMinLikeCount() != null ? request.getMinLikeCount() : 0L);
        sub.setKeywords(request.getKeywords());
        sub.setNotifyEnabled(1);
        sub.setDeleted(0);
        trendingSubscriptionMapper.insert(sub);

        log.info("Trending subscribed: userId={}, platform={}, industry={}", userId, request.getPlatform(), request.getIndustry());
        return sub;
    }

    public List<TrendingSubscription> listTrendingSubscriptions() {
        Long userId = SecurityContextHelper.currentUserId();
        return trendingSubscriptionMapper.selectList(
                new LambdaQueryWrapper<TrendingSubscription>()
                        .eq(TrendingSubscription::getUserId, userId)
                        .orderByDesc(TrendingSubscription::getCreatedAt));
    }

    // ========== Shared ==========

    public void cancelSubscription(Long id, String type) {
        Long userId = SecurityContextHelper.currentUserId();
        if ("creator".equals(type)) {
            CreatorSubscription sub = creatorSubscriptionMapper.selectById(id);
            if (sub == null || !sub.getUserId().equals(userId)) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "订阅不存在");
            }
            creatorSubscriptionMapper.deleteById(id);
        } else if ("trending".equals(type)) {
            TrendingSubscription sub = trendingSubscriptionMapper.selectById(id);
            if (sub == null || !sub.getUserId().equals(userId)) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "订阅不存在");
            }
            trendingSubscriptionMapper.deleteById(id);
        } else {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "无效的订阅类型");
        }
        log.info("Subscription cancelled: id={}, type={}", id, type);
    }

    // ========== Alerts ==========

    public Page<TrendingAlert> listAlerts(int page, int size) {
        Long userId = SecurityContextHelper.currentUserId();

        // Get user's subscription ids
        List<CreatorSubscription> creatorSubs = creatorSubscriptionMapper.selectList(
                new LambdaQueryWrapper<CreatorSubscription>().eq(CreatorSubscription::getUserId, userId)
                        .select(CreatorSubscription::getId));
        List<TrendingSubscription> trendingSubs = trendingSubscriptionMapper.selectList(
                new LambdaQueryWrapper<TrendingSubscription>().eq(TrendingSubscription::getUserId, userId)
                        .select(TrendingSubscription::getId));

        LambdaQueryWrapper<TrendingAlert> query = new LambdaQueryWrapper<>();
        // Build OR condition for subscription ids
        if (creatorSubs.isEmpty() && trendingSubs.isEmpty()) {
            // No subscriptions, return empty
            return new Page<>(page, size, 0);
        }

        query.and(w -> {
            boolean first = true;
            for (CreatorSubscription cs : creatorSubs) {
                if (first) {
                    w.eq(TrendingAlert::getSubscriptionId, cs.getId());
                    first = false;
                } else {
                    w.or().eq(TrendingAlert::getSubscriptionId, cs.getId());
                }
            }
            for (TrendingSubscription ts : trendingSubs) {
                if (first) {
                    w.eq(TrendingAlert::getSubscriptionId, ts.getId());
                    first = false;
                } else {
                    w.or().eq(TrendingAlert::getSubscriptionId, ts.getId());
                }
            }
        });
        query.orderByAsc(TrendingAlert::getIsRead).orderByDesc(TrendingAlert::getCreatedAt);

        return trendingAlertMapper.selectPage(new Page<>(page, size), query);
    }

    public void markAlertRead(Long alertId) {
        LambdaUpdateWrapper<TrendingAlert> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(TrendingAlert::getId, alertId).set(TrendingAlert::getIsRead, 1);
        trendingAlertMapper.update(null, wrapper);
    }
}
