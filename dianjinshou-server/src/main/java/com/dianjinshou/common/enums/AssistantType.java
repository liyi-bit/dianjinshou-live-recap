package com.dianjinshou.common.enums;

import com.baomidou.mybatisplus.annotation.IEnum;

public enum AssistantType implements IEnum<String> {

    OPERATION("operation", "运营助手"),
    COMPLIANCE("compliance", "违规助手"),
    SCRIPT("script", "话术助手");

    private final String code;
    private final String desc;

    AssistantType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    @Override
    public String getValue() {
        return code;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
}
