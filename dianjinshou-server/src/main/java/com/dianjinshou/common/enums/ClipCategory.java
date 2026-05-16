package com.dianjinshou.common.enums;

import com.baomidou.mybatisplus.annotation.IEnum;

public enum ClipCategory implements IEnum<String> {

    RETENTION("RETENTION", "留人切片"),
    QUALITY_SPEECH("QUALITY_SPEECH", "优质话术"),
    MARKETING("MARKETING", "营销塑品"),
    INTERACTION("INTERACTION", "互动切片"),
    FAN_CLUB("FAN_CLUB", "粉团切片"),
    EXPRESSION("EXPRESSION", "表现力切片"),
    COMPLIANCE("COMPLIANCE", "规避违规"),
    VIEWPOINT("VIEWPOINT", "观点切片"),
    EXAMPLE("EXAMPLE", "举例切片"),
    PRIVATE_DOMAIN("PRIVATE_DOMAIN", "引导私域"),
    PERSONA("PERSONA", "人设切片"),
    LOOP_SPEECH("LOOP_SPEECH", "循环话术"),
    BIE_DAN("BIE_DAN", "憋单切片"),
    OTHER("OTHER", "其他切片");

    private final String code;
    private final String desc;

    ClipCategory(String code, String desc) {
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
