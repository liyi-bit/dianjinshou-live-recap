package com.dianjinshou.common.exception;

import com.dianjinshou.common.response.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BusinessExceptionTest {

    @Test
    void constructor_withErrorCode() {
        BusinessException ex = new BusinessException(ErrorCode.NOT_FOUND);
        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
        assertNull(ex.getDetail());
        assertEquals("资源不存在", ex.getMessage());
    }

    @Test
    void constructor_withDetail() {
        BusinessException ex = new BusinessException(ErrorCode.PARAM_ERROR, "phone格式不正确");
        assertEquals(ErrorCode.PARAM_ERROR, ex.getErrorCode());
        assertEquals("phone格式不正确", ex.getDetail());
        assertEquals("参数错误: phone格式不正确", ex.getMessage());
    }

    // ========== QA 增量: 边界测试 ==========

    @Test
    void extendsRuntimeException() {
        BusinessException ex = new BusinessException(ErrorCode.INTERNAL_ERROR);
        assertInstanceOf(RuntimeException.class, ex);
        assertInstanceOf(Exception.class, ex);
    }

    @Test
    void catchable() {
        assertThrows(BusinessException.class, () -> {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        });
    }

    @Test
    void differentErrorCodes_mapCorrectly() {
        BusinessException ex1 = new BusinessException(ErrorCode.UNAUTHORIZED);
        BusinessException ex2 = new BusinessException(ErrorCode.FORBIDDEN);
        BusinessException ex3 = new BusinessException(ErrorCode.INTERNAL_ERROR);

        assertEquals(ErrorCode.UNAUTHORIZED, ex1.getErrorCode());
        assertEquals(ErrorCode.FORBIDDEN, ex2.getErrorCode());
        assertEquals(ErrorCode.INTERNAL_ERROR, ex3.getErrorCode());
    }

    @Test
    void serverErrorWithDetail() {
        BusinessException ex = new BusinessException(ErrorCode.AI_SERVICE_ERROR, "timeout");
        assertEquals("AI服务调用失败: timeout", ex.getMessage());
        assertEquals("timeout", ex.getDetail());
    }
}
