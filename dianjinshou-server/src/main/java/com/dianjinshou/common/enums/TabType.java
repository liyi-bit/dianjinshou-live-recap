package com.dianjinshou.common.enums;

import com.baomidou.mybatisplus.annotation.IEnum;

public enum TabType implements IEnum<String> {

    MINUTE_SEGMENTS("MINUTE_SEGMENTS", "分钟级分段"),
    AI_SCRIPT("AI_SCRIPT", "AI脚本"),
    RECAP_SUMMARY("RECAP_SUMMARY", "复盘总结"),
    CORRECTIONS("CORRECTIONS", "纠正记录");

    private final String code;
    private final String desc;

    TabType(String code, String desc) {
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
