package com.dianjinshou.common.response;

/**
 * 错误码枚举 — SPEC §5.2 全部 16 个错误码
 */
public enum ErrorCode {

    SUCCESS(200, "success"),

    PARAM_ERROR(40001, "参数错误"),
    BUSINESS_RULE_VIOLATION(40002, "业务规则校验失败"),

    UNAUTHORIZED(40100, "未登录或Token无效"),
    TOKEN_EXPIRED(40101, "Token已过期"),

    FORBIDDEN(40300, "无权限"),
    CROSS_LIST_COMPARISON(40301, "跨列表加入对比"),
    CROSS_ORG_ACCESS(40302, "跨组织数据访问"),
    NOT_RESOURCE_OWNER(40303, "非资源所有者"),

    NOT_FOUND(40400, "资源不存在"),
    CONFLICT(40900, "资源冲突"),
    PAYLOAD_TOO_LARGE(41300, "文件过大"),
    TOO_MANY_REQUESTS(42900, "请求过于频繁"),

    INTERNAL_ERROR(50000, "内部错误"),
    BUSINESS_INTERNAL_ERROR(50001, "业务内部错误"),
    AI_SERVICE_ERROR(50002, "AI服务调用失败"),
    DB_ERROR(50003, "数据库错误"),
    THIRD_PARTY_UNAVAILABLE(50004, "第三方服务不可用"),
    THIRD_PARTY_NOT_CONFIGURED(40005, "请先在设置→第三方接入完成配置"),
    DAILY_QUOTA_EXHAUSTED(40006, "今日 AI 复盘额度已用完");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
