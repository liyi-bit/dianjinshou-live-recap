package com.dianjinshou.modules.obs.controller;

import com.dianjinshou.common.response.ApiResponse;
import com.dianjinshou.common.response.PageResult;
import com.dianjinshou.common.security.SecurityContextHelper;
import com.dianjinshou.common.security.SecurityUser;
import com.dianjinshou.modules.obs.dto.ReportErrorRequest;
import com.dianjinshou.modules.obs.entity.ErrorLog;
import com.dianjinshou.modules.obs.service.ErrorLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

@RestController
public class ErrorLogController {

    private static final Logger log = LoggerFactory.getLogger(ErrorLogController.class);

    private final ErrorLogService service;

    public ErrorLogController(ErrorLogService service) {
        this.service = service;
    }

    /**
     * 客户端批量上报错误。无需登录（客户端异常常发生在登录前 / token 过期时），
     * 但带登录态时会带上 userId；采用 IP 级弱限流防滥用（未实现 —— 依赖 nginx 层或后续加）
     */
    @PostMapping("/api/v1/obs/error")
    public ApiResponse<Integer> report(@RequestBody ReportErrorRequest req,
                                        HttpServletRequest http) {
        SecurityUser user = SecurityContextHelper.currentUser();
        Long userId = user != null ? user.getUserId() : null;
        Long orgId = user != null ? user.getOrgId() : null;
        String ip = getClientIp(http);
        int ok = service.ingest(userId, orgId, ip, req);
        if (ok > 0) {
            log.info("obs.ingest accepted {} from user={} ip={}", ok, userId, ip);
        }
        return ApiResponse.success(ok);
    }

    /** Admin 分页查询错误日志 */
    @GetMapping("/api/v1/admin/obs/errors")
    public ApiResponse<PageResult<ErrorLog>> listForAdmin(
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String scope,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String clientVersion,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) LocalDateTime since,
            @RequestParam(required = false) LocalDateTime until,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size) {
        // 权限：sec config 里 /admin/** 要 ADMIN/SUPER_ADMIN，自然拦截
        return ApiResponse.success(service.list(
                level, scope, source, userId, clientVersion, since, until, keyword, page, size));
    }

    private static String getClientIp(HttpServletRequest req) {
        String v = req.getHeader("X-Forwarded-For");
        if (v != null && !v.isEmpty()) return v.split(",")[0].trim();
        v = req.getHeader("X-Real-IP");
        if (v != null && !v.isEmpty()) return v;
        return req.getRemoteAddr();
    }
}
