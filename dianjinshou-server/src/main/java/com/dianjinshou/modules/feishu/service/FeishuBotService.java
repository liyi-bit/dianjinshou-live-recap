package com.dianjinshou.modules.feishu.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dianjinshou.common.exception.BusinessException;
import com.dianjinshou.common.response.ErrorCode;
import com.dianjinshou.common.security.SecurityContextHelper;
import com.dianjinshou.modules.feishu.dto.CreateFeishuBotRequest;
import com.dianjinshou.modules.feishu.entity.FeishuBot;
import com.dianjinshou.modules.feishu.mapper.FeishuBotMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class FeishuBotService {

    private static final Logger log = LoggerFactory.getLogger(FeishuBotService.class);

    private final FeishuBotMapper mapper;
    private final FeishuBotLauncher launcher;

    public FeishuBotService(FeishuBotMapper mapper, FeishuBotLauncher launcher) {
        this.mapper = mapper;
        this.launcher = launcher;
    }

    public FeishuBot create(CreateFeishuBotRequest req) {
        Long userId = SecurityContextHelper.currentUserId();
        if (userId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED);

        FeishuBot existing = findByAppId(req.getAppId());
        if (existing != null) {
            throw new BusinessException(ErrorCode.CONFLICT, "该 AppId 已被其他账号绑定，不能重复添加");
        }

        FeishuBot bot = new FeishuBot();
        bot.setUserId(userId);
        bot.setAppId(req.getAppId().trim());
        bot.setAppSecret(req.getAppSecret().trim());
        bot.setBotName(req.getBotName());
        bot.setStatus(1);
        mapper.insert(bot);
        launcher.startOne(bot);
        return bot;
    }

    public List<FeishuBot> listMine() {
        Long userId = SecurityContextHelper.currentUserId();
        LambdaQueryWrapper<FeishuBot> q = new LambdaQueryWrapper<FeishuBot>()
                .eq(FeishuBot::getUserId, userId)
                .orderByDesc(FeishuBot::getCreatedAt);
        return mapper.selectList(q);
    }

    public void delete(Long id) {
        Long userId = SecurityContextHelper.currentUserId();
        FeishuBot bot = mapper.selectById(id);
        if (bot == null) throw new BusinessException(ErrorCode.NOT_FOUND, "机器人不存在");
        if (!bot.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "只能删除自己的机器人");
        }
        launcher.stopOne(bot.getAppId());
        mapper.deleteById(id);
    }

    public FeishuBot findByAppId(String appId) {
        LambdaQueryWrapper<FeishuBot> q = new LambdaQueryWrapper<FeishuBot>()
                .eq(FeishuBot::getAppId, appId);
        return mapper.selectOne(q);
    }

    public List<FeishuBot> listEnabled() {
        LambdaQueryWrapper<FeishuBot> q = new LambdaQueryWrapper<FeishuBot>()
                .eq(FeishuBot::getStatus, 1);
        return mapper.selectList(q);
    }

    /** 启动器回调：记录连接状态 */
    public void markConnected(String appId) {
        FeishuBot bot = findByAppId(appId);
        if (bot == null) return;
        bot.setLastConnectedAt(LocalDateTime.now());
        bot.setLastError(null);
        mapper.updateById(bot);
    }

    public void markError(String appId, String error) {
        FeishuBot bot = findByAppId(appId);
        if (bot == null) return;
        bot.setLastError(error != null && error.length() > 250 ? error.substring(0, 250) : error);
        mapper.updateById(bot);
    }
}
