package com.dianjinshou.common.security;

import com.dianjinshou.common.exception.BusinessException;
import com.dianjinshou.common.response.ErrorCode;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

class FileUploadValidatorTest {

    @Test
    void acceptsValidJpeg() {
        byte[] jpegHeader = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0};
        InputStream is = new ByteArrayInputStream(jpegHeader);
        assertDoesNotThrow(() ->
                FileUploadValidator.validateAvatar("image/jpeg", 1024 * 1024, is));
    }

    @Test
    void acceptsValidPng() {
        byte[] pngHeader = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A};
        InputStream is = new ByteArrayInputStream(pngHeader);
        assertDoesNotThrow(() ->
                FileUploadValidator.validateAvatar("image/png", 2 * 1024 * 1024, is));
    }

    @Test
    void rejectsOversizedFile() {
        byte[] data = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
        InputStream is = new ByteArrayInputStream(data);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> FileUploadValidator.validateAvatar("image/jpeg", 6 * 1024 * 1024, is));
        assertEquals(ErrorCode.PAYLOAD_TOO_LARGE.getCode(), ex.getErrorCode().getCode());
    }

    @Test
    void rejectsUnsupportedContentType() {
        byte[] data = new byte[]{0x25, 0x50, 0x44, 0x46};
        InputStream is = new ByteArrayInputStream(data);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> FileUploadValidator.validateAvatar("application/pdf", 1024, is));
        assertEquals(ErrorCode.PARAM_ERROR.getCode(), ex.getErrorCode().getCode());
    }

    @Test
    void rejectsMismatchedMagicNumber() {
        // Claim JPEG but provide PNG header
        byte[] pngHeader = {(byte) 0x89, 0x50, 0x4E, 0x47};
        InputStream is = new ByteArrayInputStream(pngHeader);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> FileUploadValidator.validateAvatar("image/jpeg", 1024, is));
        assertTrue(ex.getMessage().contains("不匹配"));
    }

    @Test
    void rejectsNullContentType() {
        byte[] data = new byte[]{0x00};
        InputStream is = new ByteArrayInputStream(data);

        assertThrows(BusinessException.class,
                () -> FileUploadValidator.validateAvatar(null, 1024, is));
    }
}
