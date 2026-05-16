package com.dianjinshou.common.exception;

import com.dianjinshou.common.response.ApiResponse;
import com.dianjinshou.common.response.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.servlet.NoHandlerFoundException;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("T02: GlobalExceptionHandler 全局异常处理")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    // ---- BusinessException 系列 ----

    @Test
    @DisplayName("TC-02-05-A: BusinessException(PARAM_ERROR) -> HTTP 400, code=40001")
    void businessParamError() {
        BusinessException ex = new BusinessException(ErrorCode.PARAM_ERROR);
        ResponseEntity<ApiResponse<Void>> resp = handler.handleBusinessException(ex);

        assertEquals(HttpStatus.BAD_REQUEST.value(), resp.getStatusCodeValue());
        assertEquals(40001, resp.getBody().getCode());
        assertEquals("参数错误", resp.getBody().getMessage());
    }

    @Test
    @DisplayName("TC-02-05-B: BusinessException(UNAUTHORIZED) -> HTTP 401")
    void businessUnauthorized() {
        BusinessException ex = new BusinessException(ErrorCode.UNAUTHORIZED);
        ResponseEntity<ApiResponse<Void>> resp = handler.handleBusinessException(ex);

        assertEquals(HttpStatus.UNAUTHORIZED.value(), resp.getStatusCodeValue());
        assertEquals(40100, resp.getBody().getCode());
    }

    @Test
    @DisplayName("TC-02-05-C: BusinessException(FORBIDDEN) -> HTTP 403")
    void businessForbidden() {
        BusinessException ex = new BusinessException(ErrorCode.FORBIDDEN);
        ResponseEntity<ApiResponse<Void>> resp = handler.handleBusinessException(ex);

        assertEquals(HttpStatus.FORBIDDEN.value(), resp.getStatusCodeValue());
        assertEquals(40300, resp.getBody().getCode());
    }

    @Test
    @DisplayName("TC-02-05-D: BusinessException(NOT_FOUND) -> HTTP 404")
    void businessNotFound() {
        BusinessException ex = new BusinessException(ErrorCode.NOT_FOUND);
        ResponseEntity<ApiResponse<Void>> resp = handler.handleBusinessException(ex);

        assertEquals(HttpStatus.NOT_FOUND.value(), resp.getStatusCodeValue());
        assertEquals(40400, resp.getBody().getCode());
    }

    @Test
    @DisplayName("TC-02-05-E: BusinessException with detail -> message 含 detail")
    void businessWithDetail() {
        BusinessException ex = new BusinessException(ErrorCode.PARAM_ERROR, "name 不能为空");
        ResponseEntity<ApiResponse<Void>> resp = handler.handleBusinessException(ex);

        assertEquals(40001, resp.getBody().getCode());
        assertTrue(resp.getBody().getMessage().contains("name 不能为空"));
    }

    @Test
    @DisplayName("TC-02-05-F: MethodArgumentNotValidException -> HTTP 400, code=40001")
    void methodArgumentNotValid() {
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("obj", "name", "不能为空");
        when(bindingResult.getFieldErrors()).thenReturn(Arrays.asList(fieldError));

        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);
        ResponseEntity<ApiResponse<Void>> resp = handler.handleValidation(ex);

        assertEquals(HttpStatus.BAD_REQUEST.value(), resp.getStatusCodeValue());
        assertEquals(40001, resp.getBody().getCode());
        assertTrue(resp.getBody().getMessage().contains("不能为空"));
    }

    @Test
    @DisplayName("TC-02-05-G: BindException -> HTTP 400, code=40001")
    void bindException() {
        BindException ex = mock(BindException.class);
        FieldError fieldError = new FieldError("obj", "age", "必须为正数");
        when(ex.getFieldErrors()).thenReturn(Arrays.asList(fieldError));

        ResponseEntity<ApiResponse<Void>> resp = handler.handleBind(ex);

        assertEquals(HttpStatus.BAD_REQUEST.value(), resp.getStatusCodeValue());
        assertEquals(40001, resp.getBody().getCode());
        assertTrue(resp.getBody().getMessage().contains("必须为正数"));
    }

    @Test
    @DisplayName("TC-02-05-H: ConstraintViolationException -> HTTP 400, code=40001")
    void constraintViolation() {
        @SuppressWarnings("unchecked")
        ConstraintViolation<Object> violation = mock(ConstraintViolation.class);
        when(violation.getMessage()).thenReturn("不能超过 255 字");
        Set<ConstraintViolation<?>> violations = new HashSet<>();
        violations.add(violation);

        ConstraintViolationException ex = new ConstraintViolationException(violations);
        ResponseEntity<ApiResponse<Void>> resp = handler.handleConstraint(ex);

        assertEquals(HttpStatus.BAD_REQUEST.value(), resp.getStatusCodeValue());
        assertEquals(40001, resp.getBody().getCode());
        assertTrue(resp.getBody().getMessage().contains("不能超过 255 字"));
    }

    @Test
    @DisplayName("TC-02-05-I: MissingServletRequestParameterException -> HTTP 400, 含参数名")
    void missingParam() {
        MissingServletRequestParameterException ex =
                new MissingServletRequestParameterException("userId", "String");
        ResponseEntity<ApiResponse<Void>> resp = handler.handleMissingParam(ex);

        assertEquals(HttpStatus.BAD_REQUEST.value(), resp.getStatusCodeValue());
        assertEquals(40001, resp.getBody().getCode());
        assertTrue(resp.getBody().getMessage().contains("userId"));
    }

    @Test
    @DisplayName("TC-02-05-J: NoHandlerFoundException -> HTTP 404, code=40400")
    void noHandlerFound() {
        NoHandlerFoundException ex = new NoHandlerFoundException("GET", "/api/v1/nonexist", null);
        ResponseEntity<ApiResponse<Void>> resp = handler.handleNotFound(ex);

        assertEquals(HttpStatus.NOT_FOUND.value(), resp.getStatusCodeValue());
        assertEquals(40400, resp.getBody().getCode());
    }

    @Test
    @DisplayName("TC-02-05-K: HttpRequestMethodNotSupportedException -> HTTP 405")
    void methodNotAllowed() {
        HttpRequestMethodNotSupportedException ex =
                new HttpRequestMethodNotSupportedException("DELETE");
        ResponseEntity<ApiResponse<Void>> resp = handler.handleMethodNotAllowed(ex);

        assertEquals(HttpStatus.METHOD_NOT_ALLOWED.value(), resp.getStatusCodeValue());
        assertEquals(40001, resp.getBody().getCode());
        assertTrue(resp.getBody().getMessage().contains("DELETE"));
    }

    @Test
    @DisplayName("TC-02-05-L: 未知 Exception -> HTTP 500, code=50000")
    void generalException() {
        Exception ex = new RuntimeException("unexpected");
        ResponseEntity<ApiResponse<Void>> resp = handler.handleGeneral(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), resp.getStatusCodeValue());
        assertEquals(50000, resp.getBody().getCode());
    }

    @Test
    @DisplayName("TC-02-05-M: BusinessException(CONFLICT) -> HTTP 409")
    void businessConflict() {
        BusinessException ex = new BusinessException(ErrorCode.CONFLICT);
        ResponseEntity<ApiResponse<Void>> resp = handler.handleBusinessException(ex);

        assertEquals(409, resp.getStatusCodeValue());
        assertEquals(40900, resp.getBody().getCode());
    }

    @Test
    @DisplayName("TC-02-05-N: BusinessException(PAYLOAD_TOO_LARGE) -> HTTP 413")
    void businessPayloadTooLarge() {
        BusinessException ex = new BusinessException(ErrorCode.PAYLOAD_TOO_LARGE);
        ResponseEntity<ApiResponse<Void>> resp = handler.handleBusinessException(ex);

        assertEquals(413, resp.getStatusCodeValue());
        assertEquals(41300, resp.getBody().getCode());
    }

    @Test
    @DisplayName("TC-02-05-O: BusinessException(TOO_MANY_REQUESTS) -> HTTP 429")
    void businessTooManyRequests() {
        BusinessException ex = new BusinessException(ErrorCode.TOO_MANY_REQUESTS);
        ResponseEntity<ApiResponse<Void>> resp = handler.handleBusinessException(ex);

        assertEquals(429, resp.getStatusCodeValue());
        assertEquals(42900, resp.getBody().getCode());
    }

    @Test
    @DisplayName("TC-02-05-P: BusinessException(INTERNAL_ERROR) -> HTTP 500")
    void businessInternalError() {
        BusinessException ex = new BusinessException(ErrorCode.INTERNAL_ERROR);
        ResponseEntity<ApiResponse<Void>> resp = handler.handleBusinessException(ex);

        assertEquals(500, resp.getStatusCodeValue());
        assertEquals(50000, resp.getBody().getCode());
    }

    @Test
    @DisplayName("TC-02-05-Q: BusinessException(TOKEN_EXPIRED) -> HTTP 401")
    void businessTokenExpired() {
        BusinessException ex = new BusinessException(ErrorCode.TOKEN_EXPIRED);
        ResponseEntity<ApiResponse<Void>> resp = handler.handleBusinessException(ex);

        assertEquals(401, resp.getStatusCodeValue());
        assertEquals(40101, resp.getBody().getCode());
    }

    @Test
    @DisplayName("TC-02-05-R: BusinessException(BUSINESS_RULE_VIOLATION) -> HTTP 400")
    void businessRuleViolation() {
        BusinessException ex = new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION);
        ResponseEntity<ApiResponse<Void>> resp = handler.handleBusinessException(ex);

        assertEquals(400, resp.getStatusCodeValue());
        assertEquals(40002, resp.getBody().getCode());
    }

    @Test
    @DisplayName("TC-02-05-S: BusinessException without detail -> message 不含冒号")
    void businessWithoutDetail() {
        BusinessException ex = new BusinessException(ErrorCode.NOT_FOUND);
        ResponseEntity<ApiResponse<Void>> resp = handler.handleBusinessException(ex);

        assertEquals("资源不存在", resp.getBody().getMessage());
        assertFalse(resp.getBody().getMessage().contains(":"));
    }

    @Test
    @DisplayName("TC-02-05-T: 多字段校验错误用分号拼接")
    void multipleFieldErrors() {
        BindException ex = mock(BindException.class);
        FieldError err1 = new FieldError("obj", "name", "不能为空");
        FieldError err2 = new FieldError("obj", "age", "必须大于0");
        when(ex.getFieldErrors()).thenReturn(Arrays.asList(err1, err2));

        ResponseEntity<ApiResponse<Void>> resp = handler.handleBind(ex);

        String msg = resp.getBody().getMessage();
        assertTrue(msg.contains("不能为空"));
        assertTrue(msg.contains("必须大于0"));
        assertTrue(msg.contains(";"));
    }
}
