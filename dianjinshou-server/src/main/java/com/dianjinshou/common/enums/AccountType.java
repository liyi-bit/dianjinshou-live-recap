package com.dianjinshou.common.enums;

import com.baomidou.mybatisplus.annotation.IEnum;

public enum AccountType implements IEnum<String> {

    OWN("own", "自有"),
    COMPETITOR("competitor", "竞品"),
    INDUSTRY("industry", "同行业");

    private final String code;
    private final String desc;

    AccountType(String code, String desc) {
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
