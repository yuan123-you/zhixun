package com.zhixun.security;

import com.zhixun.common.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * JWT 认证过滤器
 * 拦截请求，解析并验证 JWT 令牌
 */
@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final StringRedisTemplate stringRedisTemplate;

    /** 令牌请求头 */
    @Value("${jwt.header-string}")
    private String headerString;

    /** 令牌前缀 */
    @Value("${jwt.token-prefix}")
    private String tokenPrefix;

    /** Redis 黑名单前缀 */
    @Value("${jwt.blacklist-prefix:jwt:blacklist:}")
    private String blacklistPrefix;

    /**
     * 公开认证端点：JWT 认证过滤器不应在这些端点上设置已认证的 SecurityContext。
     * 原因：用户可能携带旧的 JWT Token 访问登录/注册接口，
     * 若 JWT 过滤器将其认证，Spring Security 的内部授权逻辑可能拒绝已认证用户访问 permitAll() 端点，
     * 导致返回 403 而非正常的登录/注册响应。
     */
    private static final String[] AUTH_PUBLIC_PATHS = {
            "/v1/auth/login",
            "/v1/auth/register",
            "/v1/auth/refresh",
            "/v1/auth/send-code",
            "/v1/auth/graph-captcha",
            "/v1/auth/forgot-password",
    };

    public JwtAuthenticationFilter(JwtUtil jwtUtil,
                                   @Autowired(required = false) StringRedisTemplate stringRedisTemplate) {
        this.jwtUtil = jwtUtil;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // 对于公开认证端点，跳过 JWT 认证处理
        // 这样已登录用户的旧 Token 不会干扰登录/注册流程
        String servletPath = request.getServletPath();
        for (String authPath : AUTH_PUBLIC_PATHS) {
            if (authPath.equals(servletPath)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String token = resolveToken(request);
            if (StringUtils.hasText(token) && jwtUtil.validateAccessToken(token)) {
                // 检查令牌是否在黑名单中（登出时加入）- Redis不可用时跳过黑名单检查
                try {
                    if (stringRedisTemplate != null) {
                        Boolean isBlacklisted = stringRedisTemplate.hasKey(blacklistPrefix + token);
                        if (Boolean.TRUE.equals(isBlacklisted)) {
                            log.debug("令牌已在黑名单中，拒绝访问");
                            filterChain.doFilter(request, response);
                            return;
                        }
                    }
                } catch (Exception redisEx) {
                    log.warn("Redis 黑名单检查失败（Redis 不可用），跳过黑名单验证: {}", redisEx.getMessage());
                }

                Claims claims = jwtUtil.parseAccessToken(token);
                if (claims != null) {
                    Long userId = jwtUtil.getUserIdFromAccessToken(token);
                    String username = jwtUtil.getUsernameFromAccessToken(token);
                    String role = jwtUtil.getRoleFromAccessToken(token);

                    // 构建权限列表
                    List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                    authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
                    if ("ADMIN".equals(role) || "SUPER_ADMIN".equals(role)) {
                        authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
                    }

                    // 构建认证信息
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            username, null, authorities);

                    // 将用户ID存入认证信息
                    authentication.setDetails(userId);

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        } catch (Exception e) {
            log.error("JWT 认证过滤器异常: {}", e.getMessage());
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 从请求头中解析令牌
     */
    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(headerString);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(tokenPrefix + " ")) {
            return bearerToken.substring((tokenPrefix + " ").length());
        }
        return null;
    }
}
