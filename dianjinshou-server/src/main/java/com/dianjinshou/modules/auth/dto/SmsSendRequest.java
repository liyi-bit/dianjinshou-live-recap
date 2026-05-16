package com.dianjinshou.modules.auth.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

public class SmsSendRequest {

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @NotBlank(message = "类型不能为空")
    @Pattern(regexp = "^(login|register)$", message = "type必须是login或register")
    private String type;

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
