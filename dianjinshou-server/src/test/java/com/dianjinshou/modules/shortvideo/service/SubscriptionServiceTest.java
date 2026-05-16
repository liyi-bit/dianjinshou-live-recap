package com.dianjinshou.modules.shortvideo.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.dianjinshou.common.exception.BusinessException;
import com.dianjinshou.common.security.SecurityUser;
import com.dianjinshou.modules.shortvideo.dto.SubscribeCreatorRequest;
import com.dianjinshou.modules.shortvideo.dto.SubscribeTrendingRequest;
import com.dianjinshou.modules.shortvideo.entity.CreatorSubscription;
import com.dianjinshou.modules.shortvideo.entity.TrendingAlert;
import com.dianjinshou.modules.shortvideo.entity.TrendingSubscription;
import com.dianjinshou.modules.shortvideo.mapper.CreatorSubscriptionMapper;
import com.dianjinshou.modules.shortvideo.mapper.TrendingAlertMapper;
import com.dianjinshou.modules.shortvideo.mapper.TrendingSubscriptionMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock
    private CreatorSubscriptionMapper creatorSubscriptionMapper;
    @Mock
    private TrendingSubscriptionMapper trendingSubscriptionMapper;
    @Mock
    private TrendingAlertMapper trendingAlertMapper;

    private SubscriptionService service;

    @BeforeAll
    static void initTableInfo() {
        try {
            MapperBuilderAssistant a1 = new MapperBuilderAssistant(new MybatisConfiguration(), "");
            a1.setCurrentNamespace("com.dianjinshou.modules.shortvideo.mapper.CreatorSubscriptionMapper");
            TableInfoHelper.initTableInfo(a1, CreatorSubscription.class);

            MapperBuilderAssistant a2 = new MapperBuilderAssistant(new MybatisConfiguration(), "");
            a2.setCurrentNamespace("com.dianjinshou.modules.shortvideo.mapper.TrendingSubscriptionMapper");
            TableInfoHelper.initTableInfo(a2, TrendingSubscription.class);

            MapperBuilderAssistant a3 = new MapperBuilderAssistant(new MybatisConfiguration(), "");
            a3.setCurrentNamespace("com.dianjinshou.modules.shortvideo.mapper.TrendingAlertMapper");
            TableInfoHelper.initTableInfo(a3, TrendingAlert.class);
        } catch (Exception ignored) { }
    }

    @BeforeEach
    void setUp() {
        service = new SubscriptionService(creatorSubscriptionMapper, trendingSubscriptionMapper, trendingAlertMapper);
        setCurrentUser(1L, "operator", 100L);
    }

    private void setCurrentUser(Long userId, String role, Long orgId) {
        SecurityUser user = new SecurityUser(userId, role, orgId);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void subscribeCreator_success() {
        when(creatorSubscriptionMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(creatorSubscriptionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(creatorSubscriptionMapper.insert(any(CreatorSubscription.class))).thenAnswer(inv -> {
            CreatorSubscription sub = inv.getArgument(0);
            sub.setId(1L);
            return 1;
        });

        SubscribeCreatorRequest req = new SubscribeCreatorRequest();
        req.setCreatorId(10L);
        CreatorSubscription result = service.subscribeCreator(req);

        assertNotNull(result);
        assertEquals(10L, result.getCreatorId());
    }

    @Test
    void subscribeCreator_duplicate_rejected() {
        when(creatorSubscriptionMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        CreatorSubscription existing = new CreatorSubscription();
        existing.setId(1L);
        when(creatorSubscriptionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);

        SubscribeCreatorRequest req = new SubscribeCreatorRequest();
        req.setCreatorId(10L);

        assertThrows(BusinessException.class, () -> service.subscribeCreator(req));
    }

    @Test
    void subscribeCreator_limitExceeded_rejected() {
        when(creatorSubscriptionMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(50L);

        SubscribeCreatorRequest req = new SubscribeCreatorRequest();
        req.setCreatorId(10L);

        assertThrows(BusinessException.class, () -> service.subscribeCreator(req));
    }

    @Test
    void subscribeTrending_success() {
        when(trendingSubscriptionMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(trendingSubscriptionMapper.insert(any(TrendingSubscription.class))).thenAnswer(inv -> {
            TrendingSubscription sub = inv.getArgument(0);
            sub.setId(1L);
            return 1;
        });

        SubscribeTrendingRequest req = new SubscribeTrendingRequest();
        req.setPlatform("douyin");
        req.setIndustry("美妆");
        req.setMinPlayCount(200000L);
        TrendingSubscription result = service.subscribeTrending(req);

        assertNotNull(result);
        assertEquals("douyin", result.getPlatform());
    }

    @Test
    void subscribeTrending_lowPlayCount_rejected() {
        when(trendingSubscriptionMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        SubscribeTrendingRequest req = new SubscribeTrendingRequest();
        req.setMinPlayCount(50000L); // Below 100000 threshold

        assertThrows(BusinessException.class, () -> service.subscribeTrending(req));
    }

    @Test
    void cancelSubscription_creatorSuccess() {
        CreatorSubscription sub = new CreatorSubscription();
        sub.setId(1L);
        sub.setUserId(1L);
        when(creatorSubscriptionMapper.selectById(1L)).thenReturn(sub);
        when(creatorSubscriptionMapper.deleteById(1L)).thenReturn(1);

        service.cancelSubscription(1L, "creator");

        verify(creatorSubscriptionMapper).deleteById(1L);
    }

    @Test
    void cancelSubscription_invalidType_rejected() {
        assertThrows(BusinessException.class, () -> service.cancelSubscription(1L, "invalid"));
    }
}
