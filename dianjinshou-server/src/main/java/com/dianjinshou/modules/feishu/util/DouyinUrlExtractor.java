package com.dianjinshou.modules.feishu.util;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 从飞书消息里抽取一个或多个抖音直播间标识。
 * 支持格式：
 *   - https://live.douyin.com/123456789
 *   - https://live.douyin.com/DNX833
 *   - live.douyin.com/xxx
 *   - https://v.douyin.com/xxx/（抖音短链）
 *   - 纯数字 / 字母数字抖音号（如 53802526428、xueliyingyu）
 *   - 批量：多个上述格式用 逗号 / 顿号 / 空格 / 换行 分隔
 *     例: "@机器人 88668880097, 53802526428, xueliyingyu"
 * v.douyin.com 短链不在后端解析 —— 交给桌面客户端走 follow-redirect。
 */
public final class DouyinUrlExtractor {

    private DouyinUrlExtractor() {}

    private static final Pattern LIVE_PATTERN = Pattern.compile(
            "(?:https?://)?live\\.douyin\\.com/([A-Za-z0-9_]+)");

    private static final Pattern SHORT_LINK_PATTERN = Pattern.compile(
            "https?://v\\.douyin\\.com/[A-Za-z0-9_-]+/?");

    // 飞书群 @机器人 会在消息文本里留占位符 @_user_N（N 为数字）
    private static final Pattern AT_MENTION_PATTERN = Pattern.compile("@_user_\\d+");

    // 分隔符：英文逗号、中文逗号、顿号、分号、空格、换行、Tab
    private static final Pattern SEPARATOR_PATTERN = Pattern.compile("[,，、;；\\s]+");

    // 合法抖音号字符集：字母/数字/下划线/句点，4-32 位
    private static final Pattern BARE_TOKEN_PATTERN = Pattern.compile("[A-Za-z0-9_.]{4,32}");
    private static final Pattern WHOLE_TOKEN_PATTERN = Pattern.compile("^[A-Za-z0-9_.]{4,32}$");

    public static final class Result {
        public final String type; // "live" | "short" | "raw"
        public final String value; // webRid / full short URL / raw token
        Result(String type, String value) { this.type = type; this.value = value; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Result)) return false;
            Result r = (Result) o;
            return type.equals(r.type) && value.equals(r.value);
        }
        @Override
        public int hashCode() { return (type + "\0" + value).hashCode(); }
    }

    /** 向后兼容：返回找到的第一个 Result，没有则 null。 */
    public static Result extract(String text) {
        List<Result> list = extractAll(text);
        return list.isEmpty() ? null : list.get(0);
    }

    /**
     * 抽取消息里所有识别到的抖音目标（URL / 短链 / 抖音号），按出现顺序返回，自动去重。
     */
    public static List<Result> extractAll(String text) {
        List<Result> out = new ArrayList<>();
        if (text == null || text.isEmpty()) return out;

        // 先剥 @mention 占位符
        String cleaned = AT_MENTION_PATTERN.matcher(text).replaceAll(" ").trim();
        if (cleaned.isEmpty()) return out;

        Set<Result> seen = new LinkedHashSet<>();

        // 1. URL / 短链：从整条消息里全局匹配（URL 内部可能含其他分隔符，不能先 split 后再找 URL）
        Matcher liveMatcher = LIVE_PATTERN.matcher(cleaned);
        while (liveMatcher.find()) {
            seen.add(new Result("live", liveMatcher.group(1)));
        }
        Matcher shortMatcher = SHORT_LINK_PATTERN.matcher(cleaned);
        while (shortMatcher.find()) {
            seen.add(new Result("short", shortMatcher.group()));
        }

        // 2. 剥掉已识别的 URL 后再找裸抖音号 —— 否则 URL 里的 accountId 会被当成独立 token 重复识别
        String residue = SHORT_LINK_PATTERN.matcher(cleaned).replaceAll(" ");
        residue = LIVE_PATTERN.matcher(residue).replaceAll(" ");

        // 3. 按分隔符切分，逐段匹配
        String[] segs = SEPARATOR_PATTERN.split(residue);
        int tokenSegCount = 0;
        int numericTokenSegCount = 0;
        for (String seg : segs) {
            String s = seg.trim();
            if (s.isEmpty()) continue;
            if (WHOLE_TOKEN_PATTERN.matcher(s).matches()) {
                tokenSegCount++;
                if (s.matches(".*\\d.*")) numericTokenSegCount++;
            }
        }
        // 批量模式：至少 2 段合法 token 且至少有一段含数字（明显是抖音号格式）
        // 仅此时才对"纯字母 token"放开（允许 "88668880097,xueliyingyu" 里的 xueliyingyu 通过）
        // 否则 "hello world" 这种英文句子会被误识别为 2 个抖音号
        boolean batchMode = tokenSegCount >= 2 && numericTokenSegCount >= 1;
        // 单 token 模式：只有一段且整体是合法 token（如 "@机器人 xueliyingyu"）
        boolean singleTokenMode = tokenSegCount == 1;

        for (String seg : segs) {
            String s = seg.trim();
            if (s.isEmpty()) continue;
            if (WHOLE_TOKEN_PATTERN.matcher(s).matches()) {
                if (batchMode || singleTokenMode || s.matches(".*\\d.*")) {
                    seen.add(new Result("raw", s));
                }
            } else if (!batchMode) {
                // 自然语言夹杂的段里找含数字的裸 token
                Matcher b = BARE_TOKEN_PATTERN.matcher(s);
                while (b.find()) {
                    String tok = b.group();
                    if (tok.length() >= 6 && tok.matches(".*\\d.*")) {
                        seen.add(new Result("raw", tok));
                    }
                }
            }
        }

        out.addAll(seen);
        return out;
    }
}
