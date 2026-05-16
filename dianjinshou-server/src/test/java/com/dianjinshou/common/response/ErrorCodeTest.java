package com.dianjinshou.common.response;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class ErrorCodeTest {

    @Test
    void allErrorCodes_haveUniqueCode() {
        List<Integer> codes = Arrays.stream(ErrorCode.values())
                .map(ErrorCode::getCode)
                .collect(Collectors.toList());
        long uniqueCount = codes.stream().distinct().count();
        assertEquals(codes.size(), uniqueCount, "ErrorCode codes must be unique");
    }

    @Test
    void allErrorCodes_haveNonEmptyMessage() {
        for (ErrorCode ec : ErrorCode.values()) {
            assertNotNull(ec.getMessage(), ec.name() + " message should not be null");
            assertFalse(ec.getMessage().isEmpty(), ec.name() + " message should not be empty");
        }
    }

    @Test
    void totalCount_is17() {
        // SPEC §5.2: 16 error codes + 1 SUCCESS + 1 BUSINESS_INTERNAL_ERROR = 18
        assertEquals(18, ErrorCode.values().length);
    }

    @Test
    void specificCodes_match() {
        assertEquals(200, ErrorCode.SUCCESS.getCode());
        assertEquals(40001, ErrorCode.PARAM_ERROR.getCode());
        assertEquals(40100, ErrorCode.UNAUTHORIZED.getCode());
        assertEquals(40101, ErrorCode.TOKEN_EXPIRED.getCode());
        assertEquals(40301, ErrorCode.CROSS_LIST_COMPARISON.getCode());
        assertEquals(40302, ErrorCode.CROSS_ORG_ACCESS.getCode());
        assertEquals(42900, ErrorCode.TOO_MANY_REQUESTS.getCode());
        assertEquals(50002, ErrorCode.AI_SERVICE_ERROR.getCode());
    }

    // ========== QA 增量: 边界测试 ==========

    @Test
    void valueOf_works() {
        assertEquals(ErrorCode.PARAM_ERROR, ErrorCode.valueOf("PARAM_ERROR"));
        assertEquals(ErrorCode.INTERNAL_ERROR, ErrorCode.valueOf("INTERNAL_ERROR"));
    }

    @Test
    void valueOf_invalidName_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> ErrorCode.valueOf("NON_EXISTENT"));
    }

    @Test
    void additionalCodes_match() {
        assertEquals(40002, ErrorCode.BUSINESS_RULE_VIOLATION.getCode());
        assertEquals("业务规则校验失败", ErrorCode.BUSINESS_RULE_VIOLATION.getMessage());
        assertEquals(40300, ErrorCode.FORBIDDEN.getCode());
        assertEquals(40303, ErrorCode.NOT_RESOURCE_OWNER.getCode());
        assertEquals(40400, ErrorCode.NOT_FOUND.getCode());
        assertEquals(40900, ErrorCode.CONFLICT.getCode());
        assertEquals(41300, ErrorCode.PAYLOAD_TOO_LARGE.getCode());
        assertEquals(50000, ErrorCode.INTERNAL_ERROR.getCode());
        assertEquals(50001, ErrorCode.BUSINESS_INTERNAL_ERROR.getCode());
        assertEquals(50003, ErrorCode.DB_ERROR.getCode());
        assertEquals(50004, ErrorCode.THIRD_PARTY_UNAVAILABLE.getCode());
    }
}
