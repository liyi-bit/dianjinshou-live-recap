package com.dianjinshou.common.security;

import com.dianjinshou.common.exception.BusinessException;
import com.dianjinshou.common.response.ErrorCode;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class FileUploadValidator {

    private static final long MAX_AVATAR_SIZE = 5 * 1024 * 1024; // 5MB

    private static final Set<String> ALLOWED_IMAGE_TYPES;
    static {
        Set<String> types = new HashSet<>();
        types.add("image/jpeg");
        types.add("image/png");
        types.add("image/gif");
        types.add("image/webp");
        ALLOWED_IMAGE_TYPES = Collections.unmodifiableSet(types);
    }

    private static final Map<String, byte[]> MAGIC_NUMBERS = new HashMap<>();
    static {
        MAGIC_NUMBERS.put("image/jpeg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF});
        MAGIC_NUMBERS.put("image/png", new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47});
        MAGIC_NUMBERS.put("image/gif", new byte[]{0x47, 0x49, 0x46});
        MAGIC_NUMBERS.put("image/webp", new byte[]{0x52, 0x49, 0x46, 0x46});
    }

    private FileUploadValidator() {}

    public static void validateAvatar(String contentType, long size, InputStream inputStream) {
        if (size > MAX_AVATAR_SIZE) {
            throw new BusinessException(ErrorCode.PAYLOAD_TOO_LARGE, "头像文件不能超过5MB");
        }

        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "不支持的图片格式，仅支持 JPEG/PNG/GIF/WebP");
        }

        validateMagicNumber(contentType.toLowerCase(), inputStream);
    }

    private static void validateMagicNumber(String contentType, InputStream inputStream) {
        byte[] expectedMagic = MAGIC_NUMBERS.get(contentType);
        if (expectedMagic == null) {
            return;
        }

        try {
            byte[] header = new byte[expectedMagic.length];
            int read = inputStream.read(header);
            if (read < expectedMagic.length) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "文件内容无效");
            }
            boolean match = true;
            for (int i = 0; i < expectedMagic.length; i++) {
                if (header[i] != expectedMagic[i]) {
                    match = false;
                    break;
                }
            }
            if (!match) {
                throw new BusinessException(ErrorCode.PARAM_ERROR,
                        "文件内容与声明类型不匹配");
            }
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "文件校验失败");
        }
    }
}
