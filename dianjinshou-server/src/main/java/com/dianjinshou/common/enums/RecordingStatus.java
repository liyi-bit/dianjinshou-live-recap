package com.dianjinshou.common.enums;

import com.baomidou.mybatisplus.annotation.IEnum;

public enum RecordingStatus implements IEnum<String> {

    MONITORING("monitoring", "监控中"),
    RECORDING("recording", "录制中"),
    COMPLETED("completed", "已完成"),
    FAILED("failed", "失败"),
    INTERRUPTED("interrupted", "录制中断"),
    DELETED("deleted", "已删除");

    private final String code;
    private final String desc;

    RecordingStatus(String code, String desc) {
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
