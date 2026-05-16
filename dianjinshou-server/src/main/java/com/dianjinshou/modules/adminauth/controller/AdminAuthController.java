package com.dianjinshou.modules.adminauth.controller;

import com.dianjinshou.common.response.ApiResponse;
import com.dianjinshou.modules.adminauth.dto.AdminLoginRequest;
import com.dianjinshou.modules.adminauth.service.AdminAuthService;
import com.dianjinshou.modules.adminauth.vo.AdminLoginVO;
import com.dianjinshou.modules.adminauth.vo.AdminMeVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

@RestController
@RequestMapping("/api/v1/admin-auth")
public class AdminAuthController {

    private final AdminAuthService adminAuthService;

    public AdminAuthController(AdminAuthService adminAuthService) {
        this.adminAuthService = adminAuthService;
    }

    @PostMapping("/login")
    public ApiResponse<AdminLoginVO> login(@Valid @RequestBody AdminLoginRequest request,
                                            HttpServletRequest httpRequest) {
        return ApiResponse.success(adminAuthService.login(request, clientIp(httpRequest)));
    }

    @GetMapping("/me")
    public ApiResponse<AdminMeVO> me() {
        return ApiResponse.success(adminAuthService.me());
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        return ApiResponse.success();
    }

    private String clientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) {
            return xff.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isEmpty()) return realIp;
        return request.getRemoteAddr();
    }
}
