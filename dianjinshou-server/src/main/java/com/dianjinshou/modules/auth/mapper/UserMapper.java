package com.dianjinshou.modules.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dianjinshou.modules.auth.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    // ---- v1.0.x 终身配额（已废弃但保留审计） ----

    /** 读取用户已使用的"默认密钥免费配额"计数，null 视为 0。 */
    @Select("SELECT COALESCE(default_ai_used, 0) FROM users WHERE id = #{userId}")
    Integer selectDefaultAiUsed(@Param("userId") Long userId);

    /** 原子 +1。返回受影响行数（应为 1）。 */
    @Update("UPDATE users SET default_ai_used = COALESCE(default_ai_used, 0) + 1 WHERE id = #{userId}")
    int incrementDefaultAiUsed(@Param("userId") Long userId);

    // ---- v1.1.0 每日 AI 复盘配额 ----

    /** 读取今日 AI 复盘已用次数。 */
    @Select("SELECT COALESCE(daily_ai_used, 0) FROM users WHERE id = #{userId}")
    Integer selectDailyAiUsed(@Param("userId") Long userId);

    /** 读取豁免标记（1=无上限）。 */
    @Select("SELECT COALESCE(ai_quota_unlimited, 0) FROM users WHERE id = #{userId}")
    Integer selectAiQuotaUnlimited(@Param("userId") Long userId);

    /** 读取下次重置时间。 */
    @Select("SELECT daily_ai_reset_at FROM users WHERE id = #{userId}")
    java.time.LocalDateTime selectDailyAiResetAt(@Param("userId") Long userId);

    /** 原子 +1 今日次数。 */
    @Update("UPDATE users SET daily_ai_used = COALESCE(daily_ai_used, 0) + 1 WHERE id = #{userId}")
    int incrementDailyAiUsed(@Param("userId") Long userId);

    /** 重置到下个零点：daily_ai_used=0, daily_ai_reset_at=#{nextReset} */
    @Update("UPDATE users SET daily_ai_used = 0, daily_ai_reset_at = #{nextReset} WHERE id = #{userId}")
    int resetDailyAiQuota(@Param("userId") Long userId,
                          @Param("nextReset") java.time.LocalDateTime nextReset);

    /** 设置豁免标记（admin 后台用）。 */
    @Update("UPDATE users SET ai_quota_unlimited = #{unlimited} WHERE id = #{userId}")
    int setAiQuotaUnlimited(@Param("userId") Long userId, @Param("unlimited") Integer unlimited);
}
