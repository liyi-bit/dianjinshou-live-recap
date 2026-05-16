package com.dianjinshou.modules.admin.service;

import com.dianjinshou.common.exception.BusinessException;
import com.dianjinshou.common.response.ErrorCode;
import com.dianjinshou.modules.auth.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 默认密钥免费配额管理。
 * <p>没有配置自己密钥的用户，最多可以完成 {@link #DEFAULT_AI_QUOTA} 个视频的 AI 解析。
 * 超过后所有新的 AI 解析任务会被拦下。</p>
 *
 * 用法：
 * <pre>
 *   Long userId = ...;
 *   // 任务开始前
 *   quota.checkBeforeAnalyze(userId);
 *   // 调用 AI 并成功保存结果后
 *   quota.consumeAfterAnalyze(userId);
 * </pre>
 */
@Service
public class DefaultAiQuotaService {

    private static final Logger log = LoggerFactory.getLogger(DefaultAiQuotaService.class);
    public static final int DEFAULT_AI_QUOTA = 5;

    private final ThirdPartySettings thirdParty;
    private final UserMapper userMapper;

    public DefaultAiQuotaService(ThirdPartySettings thirdParty, UserMapper userMapper) {
        this.thirdParty = thirdParty;
        this.userMapper = userMapper;
    }

    /**
     * 任务开始前检查：若用户用默认密钥且已用完 5 次，抛 {@link ErrorCode#THIRD_PARTY_NOT_CONFIGURED}。
     * <p>有自己密钥的用户直接放行；默认密钥用户且未用完继续流程。</p>
     */
    public void checkBeforeAnalyze(Long userId) {
        if (userId == null) return;
        if (thirdParty.hasOwnAiConfig(userId)) return;   // 用户自己配了密钥，不受限
        Integer used = userMapper.selectDefaultAiUsed(userId);
        int current = used == null ? 0 : used;
        if (current >= DEFAULT_AI_QUOTA) {
            throw new BusinessException(
                ErrorCode.THIRD_PARTY_NOT_CONFIGURED,
                "免费额度已用完（" + DEFAULT_AI_QUOTA + " 个视频 AI 解析），" +
                "请在设置→第三方接入配置您自己的云雾 API Key 后继续使用");
        }
    }

    /**
     * 任务成功后计数 +1（仅对使用默认密钥的用户）。
     * <p>注意时机：应在 AI 分析**成功完成并保存**后调用；失败任务不计数。</p>
     */
    public void consumeAfterAnalyze(Long userId) {
        if (userId == null) return;
        if (thirdParty.hasOwnAiConfig(userId)) return;  // 用自己密钥的不计数
        int rows = userMapper.incrementDefaultAiUsed(userId);
        if (rows == 1) {
            Integer now = userMapper.selectDefaultAiUsed(userId);
            log.info("User {} default AI quota consumed: {}/{}", userId, now, DEFAULT_AI_QUOTA);
        }
    }

    /** 查询某用户当前额度状态（供前端展示）。 */
    public QuotaStatus getStatus(Long userId) {
        QuotaStatus s = new QuotaStatus();
        s.setLimit(DEFAULT_AI_QUOTA);
        if (userId == null) {
            s.setUsed(0);
            s.setHasOwnConfig(false);
            return s;
        }
        s.setHasOwnConfig(thirdParty.hasOwnAiConfig(userId));
        Integer used = userMapper.selectDefaultAiUsed(userId);
        s.setUsed(used == null ? 0 : used);
        return s;
    }

    public static class QuotaStatus {
        private int used;
        private int limit;
        private boolean hasOwnConfig;

        public int getUsed() { return used; }
        public void setUsed(int used) { this.used = used; }
        public int getLimit() { return limit; }
        public void setLimit(int limit) { this.limit = limit; }
        public boolean isHasOwnConfig() { return hasOwnConfig; }
        public void setHasOwnConfig(boolean hasOwnConfig) { this.hasOwnConfig = hasOwnConfig; }
    }
}
