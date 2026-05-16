package com.dianjinshou.common.security;

import com.dianjinshou.config.JwtProperties;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtil {

    private static final String CLAIM_USER_ID = "userId";
    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_ORG_ID = "orgId";
    private static final String CLAIM_TOKEN_TYPE = "type";

    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";
    private static final String TYPE_ADMIN_ACCESS = "admin_access";

    private final JwtProperties jwtProperties;
    private final SecretKey signingKey;

    public JwtUtil(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.signingKey = Keys.hmacShaKeyFor(
                jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(Long userId, String role, Long orgId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_USER_ID, userId);
        claims.put(CLAIM_ROLE, role);
        claims.put(CLAIM_ORG_ID, orgId);
        claims.put(CLAIM_TOKEN_TYPE, TYPE_ACCESS);
        return buildToken(claims, jwtProperties.getAccessTokenTtl());
    }

    public String generateAdminAccessToken(Long adminId, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_USER_ID, adminId);
        claims.put(CLAIM_ROLE, role);
        claims.put(CLAIM_TOKEN_TYPE, TYPE_ADMIN_ACCESS);
        return buildToken(claims, jwtProperties.getAccessTokenTtl());
    }

    public boolean isAdminAccessToken(Claims claims) {
        return TYPE_ADMIN_ACCESS.equals(claims.get(CLAIM_TOKEN_TYPE, String.class));
    }

    public String generateRefreshToken(Long userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_USER_ID, userId);
        claims.put(CLAIM_TOKEN_TYPE, TYPE_REFRESH);
        return buildToken(claims, jwtProperties.getRefreshTokenTtl());
    }

    public Claims parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public boolean isAccessToken(Claims claims) {
        return TYPE_ACCESS.equals(claims.get(CLAIM_TOKEN_TYPE, String.class));
    }

    public boolean isRefreshToken(Claims claims) {
        return TYPE_REFRESH.equals(claims.get(CLAIM_TOKEN_TYPE, String.class));
    }

    public Long getUserId(Claims claims) {
        Object val = claims.get(CLAIM_USER_ID);
        if (val instanceof Number) {
            return ((Number) val).longValue();
        }
        return null;
    }

    public String getRole(Claims claims) {
        return claims.get(CLAIM_ROLE, String.class);
    }

    public Long getOrgId(Claims claims) {
        Object val = claims.get(CLAIM_ORG_ID);
        if (val instanceof Number) {
            return ((Number) val).longValue();
        }
        return null;
    }

    public long getAccessTokenTtlSeconds() {
        return jwtProperties.getAccessTokenTtl();
    }

    private String buildToken(Map<String, Object> claims, long ttlSeconds) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + ttlSeconds * 1000))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }
}
