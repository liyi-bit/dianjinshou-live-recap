package com.dianjinshou.modules.feishu.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class CreateFeishuBotRequest {

    @NotBlank(message = "AppId 不能为空")
    @Size(max = 64)
    private String appId;

    @NotBlank(message = "AppSecret 不能为空")
    @Size(max = 128)
    private String appSecret;

    @Size(max = 64)
    private String botName;

    public String getAppId() { return appId; }
    public void setAppId(String appId) { this.appId = appId; }

    public String getAppSecret() { return appSecret; }
    public void setAppSecret(String appSecret) { this.appSecret = appSecret; }

    public String getBotName() { return botName; }
    public void setBotName(String botName) { this.botName = botName; }
}
