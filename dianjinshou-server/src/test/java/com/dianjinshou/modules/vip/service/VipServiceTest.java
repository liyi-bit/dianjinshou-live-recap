package com.dianjinshou.modules.vip.service;

import com.dianjinshou.common.security.SecurityUser;
import com.dianjinshou.modules.auth.entity.User;
import com.dianjinshou.modules.auth.mapper.UserMapper;
import com.dianjinshou.modules.vip.entity.VipPlan;
import com.dianjinshou.modules.vip.mapper.VipPlanMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VipServiceTest {

    @Mock
    private VipPlanMapper vipPlanMapper;
    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private VipService vipService;

    @BeforeEach
    void setUp() {
        SecurityUser user = new SecurityUser(1L, "admin", 5L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void listPlans_success() {
        VipPlan free = new VipPlan();
        free.setId(1L);
        free.setName("免费版");
        free.setLevel(0);
        VipPlan enterprise = new VipPlan();
        enterprise.setId(2L);
        enterprise.setName("企业版");
        enterprise.setLevel(3);
        when(vipPlanMapper.selectList(any())).thenReturn(Arrays.asList(free, enterprise));

        List<VipPlan> result = vipService.listPlans();
        assertEquals(2, result.size());
    }

    @Test
    void getQuotaInfo_freeUser() {
        User user = new User();
        user.setId(1L);
        user.setVipLevel(0);
        user.setAiQuotaTotal(500000L);
        user.setAiQuotaUsed(100000L);
        user.setDurationQuotaTotal(0L);
        user.setDurationQuotaUsed(0L);
        when(userMapper.selectById(1L)).thenReturn(user);

        Map<String, Object> result = vipService.getQuotaInfo();

        assertEquals(400000L, result.get("aiQuotaRemaining"));
        assertEquals(1, result.get("maxConcurrentRecordings"));
    }

    @Test
    void getQuotaInfo_enterpriseUser() {
        User user = new User();
        user.setId(1L);
        user.setVipLevel(3);
        user.setAiQuotaTotal(5000000L);
        user.setAiQuotaUsed(0L);
        user.setDurationQuotaTotal(0L);
        user.setDurationQuotaUsed(0L);
        when(userMapper.selectById(1L)).thenReturn(user);

        Map<String, Object> result = vipService.getQuotaInfo();

        assertEquals(10, result.get("maxConcurrentRecordings"));
    }

    @Test
    void hasQuotaForAnalysis_sufficient() {
        User user = new User();
        user.setAiQuotaTotal(500000L);
        user.setAiQuotaUsed(100000L);
        when(userMapper.selectById(1L)).thenReturn(user);

        assertTrue(vipService.hasQuotaForAnalysis(1L, 50000));
    }

    @Test
    void hasQuotaForAnalysis_insufficient() {
        User user = new User();
        user.setAiQuotaTotal(500000L);
        user.setAiQuotaUsed(490000L);
        when(userMapper.selectById(1L)).thenReturn(user);

        assertFalse(vipService.hasQuotaForAnalysis(1L, 50000));
    }
}
