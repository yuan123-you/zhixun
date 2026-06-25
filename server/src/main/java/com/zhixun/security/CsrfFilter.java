package com.zhixun.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * CSRF 防护过滤器 — 双重提交 Cookie 模式
 * <p>
 * 原理：
 * 1. 对 GET 等安全请求，生成 CSRF Token 写入 Cookie（XSRF-TOKEN）
 * 2. 对 POST/PUT/DELETE 等状态变更请求，校验请求头 X-XSRF-TOKEN 与 Cookie 中的 XSRF-TOKEN 是否一致
 * 3. 攻击者无法读取跨域 Cookie 值，因此无法伪造请求
 * <p>
 * SSR 兼容：
 * - Nuxt 服务端渲染时无法携带浏览器 Cookie，因此对 SSR 发起的请求跳过 CSRF 校验
 * - 通过请求头 X-SSR-Request: true 识别 SSR 请求
 * - 内网请求（SSR 服务器）通过 X-Forwarded-For 或 RemoteAddr 识别，同样跳过校验
 * - 注意：已移除 User-Agent 检查（nuxt/axios 匹配），防止攻击者伪造 UA 绕过 CSRF 校验
 * <p>
 * SSR POST 请求支持：
 * - 当 SSR 服务端需要发起 POST 等状态变更请求时，可通过 X-SSR-Secret 请求头携带预配置的密钥
 * - 该密钥仅在服务端环境变量中配置，不会暴露给浏览器端，因此不受 CSRF 攻击影响
 * - 配置项：ssr.secret，通过环境变量 SSR_SECRET 注入
 * <p>
 * SSR CSRF Token 转发：
 * - Nuxt SSR 在代理客户端请求时，应将原始请求中的 X-XSRF-TOKEN 和 XSRF-TOKEN Cookie 转发至后端
 * - 这样 SSR 代理的请求也能通过正常的 CSRF 双重提交校验，无需额外跳过逻辑
 */
@Slf4j
@Component
public class CsrfFilter extends OncePerRequestFilter {

    private static final String CSRF_COOKIE_NAME = "XSRF-TOKEN";
    private static final String CSRF_HEADER_NAME = "X-XSRF-TOKEN";
    private static final String SSR_REQUEST_HEADER = "X-SSR-Request";
    /** SSR 服务端认证请求头，携带预配置的密钥以替代 CSRF Token */
    private static final String SSR_SECRET_HEADER = "X-SSR-Secret";

    /** SSR 服务端通信密钥，仅在服务端环境变量中配置，不暴露给浏览器 */
    @Value("${ssr.secret:}")
    private String ssrSecret;

    /** 跳过 CSRF 校验的公开接口（这些接口已有图形验证码等防刷机制，且 CSRF 攻击无法获利） */
    private static final String[] CSRF_EXCLUDED_PATHS = {
            "/v1/auth/login",
            "/v1/auth/register",
            "/v1/auth/refresh",
            "/v1/auth/send-code",
            "/v1/auth/graph-captcha",
            "/v1/auth/forgot-password",
    };

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // 对安全方法（GET/HEAD/OPTIONS/TRACE）直接放行，并确保 Cookie 中有 CSRF Token
        if (isSafeMethod(request.getMethod())) {
            ensureCsrfCookie(request, response);
            filterChain.doFilter(request, response);
            return;
        }

        // 公开认证接口跳过 CSRF 校验（首次访问时浏览器尚无 XSRF-TOKEN Cookie）
        // 使用 getServletPath() 获取不含 context-path 的路径，与排除列表匹配
        String servletPath = request.getServletPath();
        for (String excludedPath : CSRF_EXCLUDED_PATHS) {
            if (excludedPath.equals(servletPath)) {
                ensureCsrfCookie(request, response);
                filterChain.doFilter(request, response);
                return;
            }
        }

        // WebSocket 握手请求放行
        String upgrade = request.getHeader("Upgrade");
        if ("websocket".equalsIgnoreCase(upgrade)) {
            filterChain.doFilter(request, response);
            return;
        }

        // SSR 请求识别：检查 X-SSR-Request 请求头
        if (isSsrRequest(request)) {
            log.debug("SSR 请求跳过 CSRF 校验: URI={}, RemoteAddr={}",
                    request.getRequestURI(), request.getRemoteAddr());
            filterChain.doFilter(request, response);
            return;
        }

        // SSR 服务端认证：通过 X-SSR-Secret 请求头验证服务端身份
        // 当 SSR 需要发起 POST 等状态变更请求时，携带预配置的密钥以替代 CSRF Token
        if (isSsrSecretValid(request)) {
            log.debug("SSR Secret 认证通过，跳过 CSRF 校验: URI={}, RemoteAddr={}",
                    request.getRequestURI(), request.getRemoteAddr());
            filterChain.doFilter(request, response);
            return;
        }

        // 内网请求白名单：SSR 服务器发起的请求跳过 CSRF 校验
        if (isInternalNetworkRequest(request)) {
            log.debug("内网请求跳过 CSRF 校验: URI={}, RemoteAddr={}",
                    request.getRequestURI(), request.getRemoteAddr());
            filterChain.doFilter(request, response);
            return;
        }

        // 状态变更请求：校验 CSRF Token
        String cookieToken = getCsrfTokenFromCookie(request);
        String headerToken = request.getHeader(CSRF_HEADER_NAME);

        if (cookieToken == null || headerToken == null || !cookieToken.equals(headerToken)) {
            log.warn("CSRF 校验失败: IP={}, URI={}, cookieToken={}, headerToken={}",
                    request.getRemoteAddr(), request.getRequestURI(),
                    cookieToken != null ? "present" : "missing",
                    headerToken != null ? "present" : "missing");
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":403,\"message\":\"CSRF token 校验失败\",\"data\":null}");
            return;
        }

        // 校验通过，确保 Cookie 中的 Token 仍然有效
        ensureCsrfCookie(request, response);
        filterChain.doFilter(request, response);
    }

    /**
     * 判断是否为 SSR（服务端渲染）发起的请求
     * 仅通过 X-SSR-Request: true 请求头识别，不检查 User-Agent
     * 原因：攻击者可伪造 User-Agent（如设置 nuxt/axios）绕过 CSRF 校验
     */
    private boolean isSsrRequest(HttpServletRequest request) {
        String ssrHeader = request.getHeader(SSR_REQUEST_HEADER);
        return "true".equalsIgnoreCase(ssrHeader);
    }

    /**
     * 验证 SSR 服务端密钥是否有效
     * 当 SSR 服务端需要发起 POST 等状态变更请求时，通过 X-SSR-Secret 请求头携带预配置的密钥
     * 密钥仅在服务端环境变量中配置，浏览器端无法获取，因此不受 CSRF 攻击影响
     */
    private boolean isSsrSecretValid(HttpServletRequest request) {
        // 未配置 SSR 密钥则不启用此机制
        if (ssrSecret == null || ssrSecret.isEmpty()) {
            return false;
        }

        String headerSecret = request.getHeader(SSR_SECRET_HEADER);
        if (headerSecret == null || headerSecret.isEmpty()) {
            return false;
        }

        // 使用常量时间比较防止时序攻击
        return constantTimeEquals(ssrSecret, headerSecret);
    }

    /**
     * 常量时间字符串比较，防止时序攻击
     */
    private boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }

    /**
     * 判断是否为内网请求（SSR 服务器）
     * 检查 X-Forwarded-For 或 RemoteAddr 是否为内网地址
     */
    private boolean isInternalNetworkRequest(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();

        // 优先检查 X-Forwarded-For 头（反向代理场景）
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isEmpty()) {
            // X-Forwarded-For 可能有多个地址，取第一个（最原始的客户端）
            String firstAddr = forwardedFor.split(",")[0].trim();
            if (isInternalAddress(firstAddr)) {
                return true;
            }
        }

        // 检查直接连接的远程地址
        return isInternalAddress(remoteAddr);
    }

    /**
     * 判断 IP 地址是否为内网地址
     * 包括：127.0.0.0/8, 10.0.0.0/8, 172.16.0.0/12, 192.168.0.0/16, 0:0:0:0:0:0:0:1
     */
    private boolean isInternalAddress(String ip) {
        if (ip == null || ip.isEmpty()) {
            return false;
        }

        // IPv6 loopback
        if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) {
            return true;
        }

        // IPv4 地址检查
        if (ip.startsWith("127.") || ip.startsWith("10.") || ip.startsWith("192.168.")) {
            return true;
        }

        // 172.16.0.0/12 范围：172.16.x.x ~ 172.31.x.x
        if (ip.startsWith("172.")) {
            String[] parts = ip.split("\\.");
            if (parts.length >= 2) {
                try {
                    int secondOctet = Integer.parseInt(parts[1]);
                    if (secondOctet >= 16 && secondOctet <= 31) {
                        return true;
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }

        return false;
    }

    /**
     * 判断是否为安全的 HTTP 方法
     */
    private boolean isSafeMethod(String method) {
        return HttpMethod.GET.name().equalsIgnoreCase(method)
                || HttpMethod.HEAD.name().equalsIgnoreCase(method)
                || HttpMethod.OPTIONS.name().equalsIgnoreCase(method)
                || HttpMethod.TRACE.name().equalsIgnoreCase(method);
    }

    /**
     * 确保 Cookie 中存在 CSRF Token，不存在则生成并写入
     */
    private void ensureCsrfCookie(HttpServletRequest request, HttpServletResponse response) {
        String existingToken = getCsrfTokenFromCookie(request);
        if (existingToken == null) {
            String newToken = generateToken();
            Cookie cookie = new Cookie(CSRF_COOKIE_NAME, newToken);
            cookie.setPath("/");
            cookie.setHttpOnly(false); // 前端 JS 需要读取此 Cookie 值
            cookie.setSecure(request.isSecure()); // HTTPS 环境下设为 Secure
            cookie.setMaxAge(-1); // 会话级 Cookie
            response.addCookie(cookie);
        }
    }

    /**
     * 从请求 Cookie 中获取 CSRF Token
     */
    private String getCsrfTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        for (Cookie cookie : cookies) {
            if (CSRF_COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    /**
     * 生成 CSRF Token
     */
    private String generateToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
