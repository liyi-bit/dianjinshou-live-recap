package com.dianjinshou.modules.feishu.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.dianjinshou.common.entity.BaseEntity;

import java.time.LocalDateTime;

@TableName("user_feishu_bots")
public class FeishuBot extends BaseEntity {

    private Long userId;
    private String appId;
    private String appSecret;
    private String botName;
    private Integer status;
    private LocalDateTime lastConnectedAt;
    private String lastError;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getAppId() { return appId; }
    public void setAppId(String appId) { this.appId = appId; }

    public String getAppSecret() { return appSecret; }
    public void setAppSecret(String appSecret) { this.appSecret = appSecret; }

    public String getBotName() { return botName; }
    public void setBotName(String botName) { this.botName = botName; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public LocalDateTime getLastConnectedAt() { return lastConnectedAt; }
    public void setLastConnectedAt(LocalDateTime lastConnectedAt) { this.lastConnectedAt = lastConnectedAt; }

    public String getLastError() { return lastError; }
    public void setLastError(String lastError) { this.lastError = lastError; }
}
