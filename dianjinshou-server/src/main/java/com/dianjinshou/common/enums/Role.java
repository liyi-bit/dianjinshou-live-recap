package com.dianjinshou.common.enums;

import com.baomidou.mybatisplus.annotation.IEnum;

public enum Role implements IEnum<String> {

    SUPER_ADMIN("super_admin", "超管"),
    ADMIN("admin", "组织管理员"),
    OPERATOR("operator", "运营"),
    ANCHOR("anchor", "主播");

    private final String code;
    private final String desc;

    Role(String code, String desc) {
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
