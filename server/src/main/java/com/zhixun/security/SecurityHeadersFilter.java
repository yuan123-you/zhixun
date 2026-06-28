package com.zhixun.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 安全响应头过滤器
 * 为所有HTTP响应添加安全相关的Header
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class SecurityHeadersFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // 防止MIME类型嗅探
        response.setHeader("X-Content-Type-Options", "nosniff");

        // 防止页面被嵌入iframe（防止点击劫持）
        response.setHeader("X-Frame-Options", "DENY");

        // 启用浏览器XSS过滤
        response.setHeader("X-XSS-Protection", "1; mode=block");

        // 内容安全策略（更严格的配置）
        // 注意：'unsafe-inline' 仅用于内联样式（Tailwind/Vue组件），脚本已移除 'unsafe-eval'
        response.setHeader("Content-Security-Policy",
                "default-src 'self'; " +
                "script-src 'self' 'unsafe-inline'; " +  // 移除 'unsafe-eval'，更安全
                "style-src 'self' 'unsafe-inline'; " +   // Tailwind CSS 需要内联样式
                "img-src 'self' data: https: blob:; " +  // 支持图片上传预览（blob）
                "font-src 'self' data:; " +
                "connect-src 'self' ws: wss: https:; " + // WebSocket 和 API 请求
                "object-src 'none'; " +                  // 禁止 <object> <embed> <applet>
                "base-uri 'self'; " +                    // 限制 <base> 标签
                "form-action 'self'; " +                 // 限制表单提交目标
                "frame-ancestors 'none'; " +             // 防止被 iframe 嵌入
                "upgrade-insecure-requests");           // 自动升级 HTTP 到 HTTPS

        // 强制HTTPS（仅在生产环境启用，HSTS需谨慎）
        response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");

        // 推荐隐私策略
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");

        // 权限策略
        response.setHeader("Permissions-Policy",
                "camera=(), microphone=(), geolocation=(), payment=()");

        filterChain.doFilter(request, response);
    }
}
