package com.dianjinshou.common.enums;

import com.baomidou.mybatisplus.annotation.IEnum;

public enum AnalysisStatus implements IEnum<String> {

    PENDING("pending", "排队分析中"),
    ASR_PROCESSING("asr_processing", "语音转写中"),
    // v1.1.0 新增：更细粒度的逐字稿生成阶段
    //   RECORDING   → 录制中（recordings.status=recording 期间）
    //   TRANSCRIBING → 逐字稿生成中（本地 ASR 跑的时候）
    //   TRANSCRIBED  → 未分析（ASR 完成、等待用户手动触发 AI）
    RECORDING("recording", "录制中"),
    TRANSCRIBING("transcribing", "逐字稿生成中"),
    TRANSCRIBED("transcribed", "未分析"),
    // 原有状态
    AI_PROCESSING("ai_processing", "AI分析中"),
    COMPLETED("completed", "分析完成"),
    FAILED("failed", "分析失败");

    private final String code;
    private final String desc;

    AnalysisStatus(String code, String desc) {
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
