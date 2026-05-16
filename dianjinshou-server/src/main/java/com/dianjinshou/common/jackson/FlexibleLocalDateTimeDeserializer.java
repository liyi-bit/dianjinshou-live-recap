package com.dianjinshou.common.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * 柔性 LocalDateTime 反序列化器，按优先级尝试多种格式：
 *   1. "yyyy-MM-dd HH:mm:ss"（项目默认 wall-time）
 *   2. ISO_LOCAL_DATE_TIME（"2026-04-30T05:43:05" / "2026-04-30T05:43:05.942"）
 *   3. ISO_OFFSET_DATE_TIME（"2026-04-30T05:43:05.942Z" / "2026-04-30T05:43:05+08:00"）
 *      → 转 Asia/Shanghai 后取 LocalDateTime
 *
 * 主要兼容客户端 new Date().toISOString() / 服务器侧 wall-time / 不同前端工具链。
 */
public class FlexibleLocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {

    private static final DateTimeFormatter WALL_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final ZoneId CN_ZONE = ZoneId.of("Asia/Shanghai");

    @Override
    public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String text = p.getText();
        if (text == null) return null;
        text = text.trim();
        if (text.isEmpty()) return null;

        // 1. 项目默认格式
        try {
            return LocalDateTime.parse(text, WALL_TIME);
        } catch (DateTimeParseException ignore) { /* fallthrough */ }

        // 2. ISO_LOCAL_DATE_TIME（无时区，T 分隔）
        try {
            return LocalDateTime.parse(text, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (DateTimeParseException ignore) { /* fallthrough */ }

        // 3. ISO_OFFSET_DATE_TIME（"...Z" 或 "...+08:00"），转 Asia/Shanghai 时间
        try {
            return OffsetDateTime.parse(text, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                    .atZoneSameInstant(CN_ZONE)
                    .toLocalDateTime();
        } catch (DateTimeParseException ignore) { /* fallthrough */ }

        // 4. ISO_DATE_TIME（最后一搏，宽松解析）
        try {
            return LocalDateTime.parse(text, DateTimeFormatter.ISO_DATE_TIME);
        } catch (DateTimeParseException e) {
            throw new IOException("无法解析时间字符串: " + text, e);
        }
    }
}
