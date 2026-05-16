package com.dianjinshou.common.response;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ApiResponseTest {

    @Test
    void success_withData() {
        ApiResponse<String> resp = ApiResponse.success("hello");
        assertEquals(200, resp.getCode());
        assertEquals("success", resp.getMessage());
        assertEquals("hello", resp.getData());
        assertNotNull(resp.getTraceId());
        assertEquals(12, resp.getTraceId().length());
    }

    @Test
    void success_noData() {
        ApiResponse<Void> resp = ApiResponse.success();
        assertEquals(200, resp.getCode());
        assertNull(resp.getData());
    }

    @Test
    void error_withErrorCode() {
        ApiResponse<Void> resp = ApiResponse.error(ErrorCode.PARAM_ERROR);
        assertEquals(40001, resp.getCode());
        assertEquals("参数错误", resp.getMessage());
        assertNull(resp.getData());
    }

    @Test
    void error_withDetail() {
        ApiResponse<Void> resp = ApiResponse.error(ErrorCode.PARAM_ERROR, "phone格式不正确");
        assertEquals(40001, resp.getCode());
        assertEquals("参数错误: phone格式不正确", resp.getMessage());
    }

    @Test
    void error_withCodeAndMessage() {
        ApiResponse<Void> resp = ApiResponse.error(50000, "系统异常");
        assertEquals(50000, resp.getCode());
        assertEquals("系统异常", resp.getMessage());
    }

    // ========== QA 增量: 边界测试 ==========

    @Test
    void error_withNullDetail_doesNotAppend() {
        ApiResponse<Object> resp = ApiResponse.error(ErrorCode.PARAM_ERROR, null);
        assertEquals(40001, resp.getCode());
        assertEquals("参数错误", resp.getMessage());
    }

    @Test
    void error_withEmptyDetail_doesNotAppend() {
        ApiResponse<Object> resp = ApiResponse.error(ErrorCode.PARAM_ERROR, "");
        assertEquals(40001, resp.getCode());
        assertEquals("参数错误", resp.getMessage());
    }

    @Test
    void getterSetter_allFields() {
        ApiResponse<String> resp = new ApiResponse<>();
        resp.setCode(500);
        resp.setMessage("err");
        resp.setData("d");
        resp.setTraceId("abc123");

        assertEquals(500, resp.getCode());
        assertEquals("err", resp.getMessage());
        assertEquals("d", resp.getData());
        assertEquals("abc123", resp.getTraceId());
    }

    @Test
    void success_withComplexObject_sameReference() {
        java.util.Map<String, Integer> map = new java.util.HashMap<>();
        map.put("count", 42);
        ApiResponse<java.util.Map<String, Integer>> resp = ApiResponse.success(map);
        assertSame(map, resp.getData());
    }

    @Test
    void uniqueTraceId_perCall() {
        ApiResponse<Object> r1 = ApiResponse.success();
        ApiResponse<Object> r2 = ApiResponse.success();
        assertNotEquals(r1.getTraceId(), r2.getTraceId(),
                "每次调用应生成不同 traceId");
    }
}
