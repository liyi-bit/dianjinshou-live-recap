package com.dianjinshou.common.security;

import java.util.regex.Pattern;

public final class LogSanitizer {

    private static final Pattern PHONE_PATTERN = Pattern.compile("(1[3-9]\\d)\\d{4}(\\d{4})");
    private static final Pattern JWT_PATTERN = Pattern.compile("(eyJ[A-Za-z0-9_-]{10})[A-Za-z0-9_.-]+");
    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("(\"(?:password|passwordHash|oldPassword|newPassword)\"\\s*:\\s*\")([^\"]+)(\")",
                    Pattern.CASE_INSENSITIVE);

    private LogSanitizer() {}

    public static String sanitize(String input) {
        if (input == null) return null;
        String result = PHONE_PATTERN.matcher(input).replaceAll("$1****$2");
        result = JWT_PATTERN.matcher(result).replaceAll("$1***[REDACTED]");
        result = PASSWORD_PATTERN.matcher(result).replaceAll("$1***[REDACTED]$3");
        return result;
    }
}
