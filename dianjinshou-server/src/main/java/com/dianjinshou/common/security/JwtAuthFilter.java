package com.dianjinshou.common.security;

import com.dianjinshou.common.response.ApiResponse;
import com.dianjinshou.common.response.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    public JwtAuthFilter(JwtUtil jwtUtil, ObjectMapper objectMapper) {
        this.jwtUtil = jwtUtil;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        // /auth/refresh 只认 body 里的 refreshToken，不校验 Authorization header
        // 否则客户端带着过期的 access token 来刷新，会被这里先拦下抛 401/40101，
        // 前端误以为 refresh token 过期被迫登出
        String path = request.getRequestURI();
        if (path != null && path.endsWith("/auth/refresh")) {
            filterChain.doFilter(request, response);
            return;
        }
        // /api/v1/public/** 是公开接口（官网看板 / 客户端版本查询等），绝不该因为
        // 客户端偶然带了过期 token 就被拦下；/api/v1/obs/error 同理（客户端崩溃上报）
        if (path != null && (path.contains("/api/v1/public/")
                || path.contains("/api/v1/obs/error"))) {
            filterChain.doFilter(request, response);
            return;
        }

        String header = request.getHeader("Authorization");

        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(BEARER_PREFIX.length());

        try {
            Claims claims = jwtUtil.parseToken(token);

            if (!jwtUtil.isAccessToken(claims) && !jwtUtil.isAdminAccessToken(claims)) {
                writeError(response, ErrorCode.UNAUTHORIZED);
                return;
            }

            Long userId = jwtUtil.getUserId(claims);
            String role = jwtUtil.getRole(claims);
            Long orgId = jwtUtil.getOrgId(claims);

            SecurityUser securityUser = new SecurityUser(userId, role, orgId);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            securityUser, null,
                            Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase())));

            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (ExpiredJwtException e) {
            writeError(response, ErrorCode.TOKEN_EXPIRED);
            return;
        } catch (JwtException e) {
            writeError(response, ErrorCode.UNAUTHORIZED);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void writeError(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        int httpStatus = mapToHttpStatus(errorCode.getCode());
        response.setStatus(httpStatus);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        ApiResponse<?> body = ApiResponse.error(errorCode);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }

    private int mapToHttpStatus(int bizCode) {
        if (bizCode == 200) return 200;
        int prefix = bizCode / 100;
        if (prefix == 401) return 401;
        if (prefix == 403) return 403;
        if (prefix >= 500) return 500;
        return 400;
    }
}
