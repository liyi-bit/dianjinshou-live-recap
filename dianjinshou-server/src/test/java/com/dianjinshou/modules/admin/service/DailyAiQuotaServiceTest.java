package com.dianjinshou.modules.admin.service;

import com.dianjinshou.modules.auth.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DailyAiQuotaServiceTest {

    @Mock
    private UserMapper userMapper;

    private DailyAiQuotaService service;

    @BeforeEach
    void setUp() {
        service = new DailyAiQuotaService(userMapper);
    }

    @Test
    void getStatus_rollsOverAfterChinaMidnight() {
        service.setClockForTest(Clock.fixed(
                Instant.parse("2026-04-26T16:05:00Z"),
                ZoneId.of("UTC")
        ));
        Long userId = 1L;
        LocalDateTime expiredReset = LocalDateTime.of(2026, 4, 27, 0, 0);
        LocalDateTime nextReset = LocalDateTime.of(2026, 4, 28, 0, 0);

        when(userMapper.selectAiQuotaUnlimited(userId)).thenReturn(0);
        when(userMapper.selectDailyAiResetAt(userId)).thenReturn(expiredReset, nextReset);
        when(userMapper.selectDailyAiUsed(userId)).thenReturn(0);

        DailyAiQuotaService.DailyQuotaStatus status = service.getStatus(userId);

        verify(userMapper).resetDailyAiQuota(userId, nextReset);
        assertEquals(0, status.getUsed());
        assertEquals(nextReset, status.getResetAt());
    }

    @Test
    void getStatus_keepsQuotaBeforeChinaMidnight() {
        service.setClockForTest(Clock.fixed(
                Instant.parse("2026-04-27T15:59:00Z"),
                ZoneId.of("UTC")
        ));
        Long userId = 1L;
        LocalDateTime resetAt = LocalDateTime.of(2026, 4, 28, 0, 0);

        when(userMapper.selectAiQuotaUnlimited(userId)).thenReturn(0);
        when(userMapper.selectDailyAiResetAt(userId)).thenReturn(resetAt);
        when(userMapper.selectDailyAiUsed(userId)).thenReturn(10);

        DailyAiQuotaService.DailyQuotaStatus status = service.getStatus(userId);

        verify(userMapper, never()).resetDailyAiQuota(userId, resetAt);
        assertEquals(10, status.getUsed());
        assertEquals(resetAt, status.getResetAt());
    }

    @Test
    void checkBeforeAnalyze_rollsOverAtExactResetTime() {
        service.setClockForTest(Clock.fixed(
                Instant.parse("2026-04-26T16:00:00Z"),
                ZoneId.of("UTC")
        ));
        Long userId = 1L;
        LocalDateTime resetAt = LocalDateTime.of(2026, 4, 27, 0, 0);
        LocalDateTime nextReset = LocalDateTime.of(2026, 4, 28, 0, 0);

        when(userMapper.selectAiQuotaUnlimited(userId)).thenReturn(0);
        when(userMapper.selectDailyAiResetAt(userId)).thenReturn(resetAt);
        when(userMapper.selectDailyAiUsed(userId)).thenReturn(0);

        service.checkBeforeAnalyze(userId);

        verify(userMapper).resetDailyAiQuota(userId, nextReset);
    }
}
