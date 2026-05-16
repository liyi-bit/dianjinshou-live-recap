package com.dianjinshou.common.enums;

import com.baomidou.mybatisplus.annotation.IEnum;

public enum RecapType implements IEnum<String> {

    FULL("full", "整场复盘"),
    CLIP("clip", "切片复盘");

    private final String code;
    private final String desc;

    RecapType(String code, String desc) {
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
