package com.dianjinshou.modules.auth.controller;

import com.dianjinshou.common.response.ApiResponse;
import com.dianjinshou.modules.auth.dto.*;
import com.dianjinshou.modules.auth.service.AuthService;
import com.dianjinshou.modules.auth.service.SmsService;
import com.dianjinshou.modules.auth.vo.LoginVO;
import com.dianjinshou.modules.auth.vo.MeVO;
import com.dianjinshou.modules.auth.vo.TokenVO;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final SmsService smsService;

    public AuthController(AuthService authService, SmsService smsService) {
        this.authService = authService;
        this.smsService = smsService;
    }

    @PostMapping("/login")
    public ApiResponse<LoginVO> login(@Valid @RequestBody LoginRequest request,
                                      HttpServletRequest httpRequest) {
        LoginVO vo = authService.login(request, getClientIp(httpRequest),
                httpRequest.getHeader("User-Agent"));
        return ApiResponse.success(vo);
    }

    @PostMapping("/login/sms")
    public ApiResponse<LoginVO> smsLogin(@Valid @RequestBody SmsLoginRequest request,
                                         HttpServletRequest httpRequest) {
        LoginVO vo = authService.smsLogin(request, getClientIp(httpRequest),
                httpRequest.getHeader("User-Agent"));
        return ApiResponse.success(vo);
    }

    @PostMapping("/register")
    public ApiResponse<LoginVO> register(@Valid @RequestBody RegisterRequest request,
                                         HttpServletRequest httpRequest) {
        LoginVO vo = authService.register(request, getClientIp(httpRequest),
                httpRequest.getHeader("User-Agent"));
        return ApiResponse.success(vo);
    }

    @PostMapping("/sms/send")
    public ApiResponse<Map<String, Object>> sendSms(@Valid @RequestBody SmsSendRequest request,
                                                    HttpServletRequest httpRequest) {
        int expireSeconds = smsService.sendVerifyCode(request.getPhone(), request.getType(),
                getClientIp(httpRequest));
        Map<String, Object> data = new java.util.HashMap<>();
        data.put("success", true);
        data.put("expireSeconds", expireSeconds);
        return ApiResponse.success(data);
    }

    @PostMapping("/refresh")
    public ApiResponse<TokenVO> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        TokenVO vo = authService.refreshToken(request);
        return ApiResponse.success(vo);
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        authService.logout();
        return ApiResponse.success();
    }

    @PostMapping("/init-org")
    public ApiResponse<Map<String, Object>> initOrg(@RequestBody Map<String, Object> body) {
        Long userId = ((Number) body.get("userId")).longValue();
        Map<String, Object> result = authService.initUserOrg(userId);
        return ApiResponse.success(result);
    }

    @GetMapping("/me")
    public ApiResponse<MeVO> me() {
        MeVO vo = authService.me();
        return ApiResponse.success(vo);
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) {
            return xff.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isEmpty()) {
            return realIp;
        }
        return request.getRemoteAddr();
    }
}
