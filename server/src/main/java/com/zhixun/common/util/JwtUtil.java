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
import java.util.Base64;
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

    /** 已知的默认密钥（来自 application.yml 默认值），生产环境不应使用 */
    private static final String DEFAULT_ACCESS_SECRET = "sdOJgjRUoN/+6k2rJMlZOilv1uTKJfQJ9uoG/8fmPok=";
    private static final String DEFAULT_REFRESH_SECRET = "STApEN/Ne/o9C+g7A8CnMM31+ChzquUR8GFXH97l7v4=";

    /**
     * 初始化时验证 JWT 密钥长度
     * 确保密钥至少 32 字节（256 位），符合 HS256 算法要求
     */
    @jakarta.annotation.PostConstruct
    public void validateSecrets() {
        validateSecretLength("JWT Access Secret", accessSecret, 32);
        validateSecretLength("JWT Refresh Secret", refreshSecret, 32);

        // 生产环境警告：检测是否使用了 application.yml 中的默认硬编码密钥
        if (DEFAULT_ACCESS_SECRET.equals(accessSecret)) {
            log.warn("===========================================");
            log.warn("⚠ 安全警告：JWT_ACCESS_SECRET 使用了默认硬编码密钥！");
            log.warn("   请在生产环境中通过环境变量 JWT_ACCESS_SECRET 设置强随机密钥。");
            log.warn("   生成命令：openssl rand -base64 32");
            log.warn("===========================================");
        }
        if (DEFAULT_REFRESH_SECRET.equals(refreshSecret)) {
            log.warn("===========================================");
            log.warn("⚠ 安全警告：JWT_REFRESH_SECRET 使用了默认硬编码密钥！");
            log.warn("   请在生产环境中通过环境变量 JWT_REFRESH_SECRET 设置强随机密钥。");
            log.warn("   生成命令：openssl rand -base64 32");
            log.warn("===========================================");
        }
    }

    /**
     * 验证密钥长度
     *
     * @param name        密钥名称（用于日志）
     * @param secret     密钥字符串
     * @param minBytes   最小字节数
     */
    private void validateSecretLength(String name, String secret, int minBytes) {
        if (secret == null || secret.isEmpty()) {
            throw new IllegalStateException(name + " 未配置，请在环境变量或配置文件中设置");
        }

        // 尝试 Base64 解码（如果配置的是 Base64 编码的密钥）
        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(secret);
        } catch (IllegalArgumentException e) {
            // 不是 Base64，使用原始字节
            keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        }

        if (keyBytes.length < minBytes) {
            throw new IllegalStateException(
                    String.format("%s 长度不足！当前：%d 字节，要求至少：%d 字节（%d 位）。请使用环境变量 JWT_ACCESS_SECRET 和 JWT_REFRESH_SECRET 配置强随机密钥。",
                            name, keyBytes.length, minBytes, minBytes * 8)
            );
        }

        log.info("{} 验证通过，密钥长度：{} 字节（{} 位）", name, keyBytes.length, keyBytes.length * 8);
    }

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
