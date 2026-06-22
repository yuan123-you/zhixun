package com.zhixun.security;

import com.zhixun.common.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
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

    public JwtAuthenticationFilter(JwtUtil jwtUtil,
                                   @Autowired(required = false) StringRedisTemplate stringRedisTemplate) {
        this.jwtUtil = jwtUtil;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String token = resolveToken(request);
            if (StringUtils.hasText(token) && jwtUtil.validateAccessToken(token)) {
                // 检查令牌是否在黑名单中（登出时加入）- Redis不可用时跳过黑名单检查
                if (stringRedisTemplate != null) {
                    Boolean isBlacklisted = stringRedisTemplate.hasKey(blacklistPrefix + token);
                    if (Boolean.TRUE.equals(isBlacklisted)) {
                        log.debug("令牌已在黑名单中，拒绝访问");
                        filterChain.doFilter(request, response);
                        return;
                    }
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
