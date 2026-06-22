package com.zhixun.common.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 工具类
 * 支持双 Token 机制（AccessToken + RefreshToken）
 */
@Slf4j
@Component
public class JwtUtil {

    /** 访问令牌密钥 */
    @Value("${jwt.access-secret}")
    private String accessSecret;

    /** 刷新令牌密钥 */
    @Value("${jwt.refresh-secret}")
    private String refreshSecret;

    /** 访问令牌过期时间（秒） */
    @Value("${jwt.access-expiration}")
    private Long accessExpiration;

    /** 刷新令牌过期时间（秒） */
    @Value("${jwt.refresh-expiration}")
    private Long refreshExpiration;

    /** 令牌类型 - 访问令牌 */
    public static final String TOKEN_TYPE_ACCESS = "access";

    /** 令牌类型 - 刷新令牌 */
    public static final String TOKEN_TYPE_REFRESH = "refresh";

    /** 自定义声明 - 用户ID */
    public static final String CLAIM_USER_ID = "userId";

    /** 自定义声明 - 用户名 */
    public static final String CLAIM_USERNAME = "username";

    /** 自定义声明 - 令牌类型 */
    public static final String CLAIM_TOKEN_TYPE = "tokenType";

    /** 自定义声明 - 用户角色 */
    public static final String CLAIM_ROLE = "role";

    /**
     * 生成访问令牌
     *
     * @param userId   用户ID
     * @param username 用户名
     * @return 访问令牌
     */
    public String generateAccessToken(Long userId, String username) {
        return generateToken(userId, username, "user", TOKEN_TYPE_ACCESS, accessSecret, accessExpiration);
    }

    /**
     * 生成访问令牌（带角色）
     *
     * @param userId   用户ID
     * @param username 用户名
     * @param role     用户角色
     * @return 访问令牌
     */
    public String generateAccessToken(Long userId, String username, String role) {
        return generateToken(userId, username, role, TOKEN_TYPE_ACCESS, accessSecret, accessExpiration);
    }

    /**
     * 生成刷新令牌
     *
     * @param userId   用户ID
     * @param username 用户名
     * @return 刷新令牌
     */
    public String generateRefreshToken(Long userId, String username) {
        return generateToken(userId, username, "user", TOKEN_TYPE_REFRESH, refreshSecret, refreshExpiration);
    }

    /**
     * 生成刷新令牌（带角色）
     *
     * @param userId   用户ID
     * @param username 用户名
     * @param role     用户角色
     * @return 刷新令牌
     */
    public String generateRefreshToken(Long userId, String username, String role) {
        return generateToken(userId, username, role, TOKEN_TYPE_REFRESH, refreshSecret, refreshExpiration);
    }

    /**
     * 生成令牌
     */
    private String generateToken(Long userId, String username, String role, String tokenType, String secret, Long expiration) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_USER_ID, userId);
        claims.put(CLAIM_USERNAME, username);
        claims.put(CLAIM_TOKEN_TYPE, tokenType);
        claims.put(CLAIM_ROLE, role);

        Date now = new Date();
        Date expirationDate = new Date(now.getTime() + expiration * 1000);

        return Jwts.builder()
                .claims(claims)
                .subject(username)
                .issuedAt(now)
                .expiration(expirationDate)
                .signWith(getSigningKey(secret))
                .compact();
    }

    /**
     * 解析访问令牌
     */
    public Claims parseAccessToken(String token) {
        return parseToken(token, accessSecret);
    }

    /**
     * 解析刷新令牌
     */
    public Claims parseRefreshToken(String token) {
        return parseToken(token, refreshSecret);
    }

    /**
     * 解析令牌
     */
    private Claims parseToken(String token, String secret) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey(secret))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException e) {
            log.warn("解析令牌失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 验证访问令牌是否有效
     */
    public boolean validateAccessToken(String token) {
        Claims claims = parseAccessToken(token);
        return claims != null
                && TOKEN_TYPE_ACCESS.equals(claims.get(CLAIM_TOKEN_TYPE))
                && !isExpired(claims);
    }

    /**
     * 验证刷新令牌是否有效
     */
    public boolean validateRefreshToken(String token) {
        Claims claims = parseRefreshToken(token);
        return claims != null
                && TOKEN_TYPE_REFRESH.equals(claims.get(CLAIM_TOKEN_TYPE))
                && !isExpired(claims);
    }

    /**
     * 从访问令牌中获取用户ID
     */
    public Long getUserIdFromAccessToken(String token) {
        Claims claims = parseAccessToken(token);
        if (claims == null) {
            return null;
        }
        Object userId = claims.get(CLAIM_USER_ID);
        if (userId instanceof Integer) {
            return ((Integer) userId).longValue();
        }
        if (userId instanceof Long) {
            return (Long) userId;
        }
        return null;
    }

    /**
     * 从访问令牌中获取用户名
     */
    public String getUsernameFromAccessToken(String token) {
        Claims claims = parseAccessToken(token);
        return claims != null ? claims.get(CLAIM_USERNAME, String.class) : null;
    }

    /**
     * 从访问令牌中获取用户角色
     *
     * @param token 访问令牌
     * @return 用户角色
     */
    public String getRoleFromAccessToken(String token) {
        Claims claims = parseAccessToken(token);
        return claims != null ? claims.get(CLAIM_ROLE, String.class) : null;
    }

    /**
     * 判断令牌是否过期
     */
    private boolean isExpired(Claims claims) {
        return claims.getExpiration().before(new Date());
    }

    /**
     * 获取签名密钥
     */
    private SecretKey getSigningKey(String secret) {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
