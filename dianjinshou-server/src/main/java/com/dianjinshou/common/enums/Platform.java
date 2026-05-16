package com.dianjinshou.common.enums;

import com.baomidou.mybatisplus.annotation.IEnum;

public enum Platform implements IEnum<String> {

    DOUYIN("douyin", "抖音"),
    KUAISHOU("kuaishou", "快手"),
    SHIPINHAO("shipinhao", "视频号");

    private final String code;
    private final String desc;

    Platform(String code, String desc) {
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
