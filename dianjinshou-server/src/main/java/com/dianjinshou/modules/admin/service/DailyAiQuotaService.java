package com.dianjinshou.modules.admin.service;

import com.dianjinshou.common.exception.BusinessException;
import com.dianjinshou.common.response.ErrorCode;
import com.dianjinshou.modules.auth.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

/**
 * 每日 AI 复盘配额服务（v1.1.0）。
 *
 * 规则：
 *   - 每个账号每天最多 {@link #DAILY_LIMIT} 次 AI 复盘
 *   - users.ai_quota_unlimited = 1 的账号豁免（白名单）
 *   - 重置时间点：Asia/Shanghai 本地零点
 *
 * 用法：
 * <pre>
 *   quota.checkBeforeAnalyze(userId);   // 入口处校验（会顺带完成"到点重置"）
 *   // ... 触发 AI 分析 ...
 *   quota.consumeAfterAnalyze(userId);  // 分析完成后 +1
 * </pre>
 */
@Service
public class DailyAiQuotaService {

    private static final Logger log = LoggerFactory.getLogger(DailyAiQuotaService.class);
    public static final int DAILY_LIMIT = 10;
    private static final ZoneId CN_TZ = ZoneId.of("Asia/Shanghai");

    private final UserMapper userMapper;
    private Clock clock = Clock.system(CN_TZ);

    public DailyAiQuotaService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    /** 任务触发前检查：配额用完抛业务异常（客户端据此弹窗提示）。自动完成"到点重置"。 */
    public void checkBeforeAnalyze(Long userId) {
        if (userId == null) return;
        if (isUnlimited(userId)) return;

        rollOverIfDue(userId);

        Integer used = userMapper.selectDailyAiUsed(userId);
        int current = used == null ? 0 : used;
        if (current >= DAILY_LIMIT) {
            throw new BusinessException(
                    ErrorCode.DAILY_QUOTA_EXHAUSTED,
                    "今日 AI 复盘额度已用完（" + DAILY_LIMIT + "/" + DAILY_LIMIT + "），明天 0 点自动重置");
        }
    }

    /** 分析成功完成 +1；豁免用户不计。 */
    public void consumeAfterAnalyze(Long userId) {
        if (userId == null) return;
        if (isUnlimited(userId)) return;
        rollOverIfDue(userId);
        int rows = userMapper.incrementDailyAiUsed(userId);
        if (rows == 1) {
            Integer now = userMapper.selectDailyAiUsed(userId);
            log.info("User {} daily AI quota consumed: {}/{}", userId, now, DAILY_LIMIT);
        }
    }

    /** 查询当前状态（供前端显示 "已用 X/10 · 明日 0 点重置"）。 */
    public DailyQuotaStatus getStatus(Long userId) {
        DailyQuotaStatus s = new DailyQuotaStatus();
        s.setLimit(DAILY_LIMIT);
        if (userId == null) {
            s.setUsed(0);
            s.setUnlimited(false);
            s.setResetAt(nextResetAt());
            return s;
        }
        s.setUnlimited(isUnlimited(userId));
        if (s.isUnlimited()) {
            s.setUsed(0);
            s.setResetAt(nextResetAt());
            return s;
        }
        rollOverIfDue(userId);
        Integer used = userMapper.selectDailyAiUsed(userId);
        s.setUsed(used == null ? 0 : used);
        LocalDateTime resetAt = userMapper.selectDailyAiResetAt(userId);
        s.setResetAt(resetAt != null ? resetAt : nextResetAt());
        return s;
    }

    /** 若 reset_at 已过 → 归零 + 设置下次 reset_at。 */
    private void rollOverIfDue(Long userId) {
        LocalDateTime resetAt = userMapper.selectDailyAiResetAt(userId);
        LocalDateTime now = nowInChina();
        if (resetAt == null || !resetAt.isAfter(now)) {
            userMapper.resetDailyAiQuota(userId, nextResetAt(now));
        }
    }

    /** 下一个零点（东八区）—— now 若已在今日零点后则返回明日零点。 */
    private LocalDateTime nextResetAt() {
        return nextResetAt(nowInChina());
    }

    private LocalDateTime nextResetAt(LocalDateTime now) {
        LocalDate tomorrow = now.toLocalDate().plusDays(1);
        return LocalDateTime.of(tomorrow, LocalTime.MIDNIGHT);
    }

    private LocalDateTime nowInChina() {
        return LocalDateTime.now(clock.withZone(CN_TZ));
    }

    void setClockForTest(Clock clock) {
        this.clock = clock == null ? Clock.system(CN_TZ) : clock;
    }

    private boolean isUnlimited(Long userId) {
        Integer v = userMapper.selectAiQuotaUnlimited(userId);
        return v != null && v == 1;
    }

    // ---- DTO ----
    public static class DailyQuotaStatus {
        private int used;
        private int limit;
        private boolean unlimited;
        private LocalDateTime resetAt;

        public int getUsed() { return used; }
        public void setUsed(int used) { this.used = used; }
        public int getLimit() { return limit; }
        public void setLimit(int limit) { this.limit = limit; }
        public boolean isUnlimited() { return unlimited; }
        public void setUnlimited(boolean unlimited) { this.unlimited = unlimited; }
        public LocalDateTime getResetAt() { return resetAt; }
        public void setResetAt(LocalDateTime resetAt) { this.resetAt = resetAt; }
    }
}
