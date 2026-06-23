package com.zhixun.security;

import com.zhixun.entity.SecurityAuditLog;
import com.zhixun.mapper.SecurityAuditLogMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 安全审计日志记录器
 * 记录所有认证失败、权限拒绝、越权尝试等安全事件
 * 同时输出到控制台日志和持久化到数据库
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SecurityAuditLogger {

    private static final String AUDIT_LOGGER_NAME = "SECURITY_AUDIT";

    private final SecurityAuditLogMapper securityAuditLogMapper;

    /**
     * 记录认证失败事件
     *
     * @param request HTTP请求
     * @param reason  失败原因
     */
    public void logAuthenticationFailed(HttpServletRequest request, String reason) {
        logAuditEvent("AUTH_FAILED", request, reason, null);
    }

    /**
     * 记录权限拒绝事件
     *
     * @param request   HTTP请求
     * @param eventType 事件类型描述
     * @param detail    详情
     */
    public void logPermissionDenied(HttpServletRequest request, String eventType, String detail) {
        logAuditEvent(eventType, request, detail, null);
    }

    /**
     * 记录越权尝试事件
     *
     * @param request HTTP请求
     * @param userId  尝试越权的用户ID
     * @param detail  详情
     */
    public void logUnauthorizedAccess(HttpServletRequest request, Long userId, String detail) {
        logAuditEvent("UNAUTHORIZED_ACCESS", request, detail, userId);
    }

    /**
     * 记录垂直越权尝试（普通用户访问管理功能）
     *
     * @param request HTTP请求
     * @param detail  详情
     */
    public void logVerticalBreachAttempt(HttpServletRequest request, String detail) {
        logAuditEvent("VERTICAL_BREACH", request, detail, null);
    }

    /**
     * 记录水平越权尝试（用户操作他人数据）
     *
     * @param request HTTP请求
     * @param userId  尝试越权的用户ID
     * @param detail  详情
     */
    public void logHorizontalBreachAttempt(HttpServletRequest request, Long userId, String detail) {
        logAuditEvent("HORIZONTAL_BREACH", request, detail, userId);
    }

    /**
     * 通用安全审计事件记录
     * 同时输出控制台日志和异步持久化到数据库
     *
     * @param eventType 事件类型
     * @param request   HTTP请求
     * @param detail    详情
     * @param userId    用户ID（可为null）
     */
    private void logAuditEvent(String eventType, HttpServletRequest request, String detail, Long userId) {
        String ip = getClientIp(request);
        String uri = request.getRequestURI();
        String method = request.getMethod();

        // 控制台日志
        log.warn("[{}] 时间:{} | IP:{} | 用户ID:{} | 事件:{} | 方法:{} | 路径:{} | 详情:{}",
                AUDIT_LOGGER_NAME,
                LocalDateTime.now(),
                ip,
                userId != null ? userId : "unknown",
                eventType,
                method,
                uri,
                detail);

        // 异步持久化到数据库
        persistAuditLog(eventType, userId, ip, method, uri, detail);
    }

    /**
     * 异步持久化审计日志到数据库
     */
    @Async
    public void persistAuditLog(String eventType, Long userId, String ip, String method, String path, String detail) {
        try {
            SecurityAuditLog auditLog = new SecurityAuditLog();
            auditLog.setEventType(eventType);
            auditLog.setUserId(userId);
            auditLog.setIp(ip);
            auditLog.setMethod(method);
            auditLog.setPath(path);
            auditLog.setDetail(detail);
            securityAuditLogMapper.insert(auditLog);
        } catch (Exception e) {
            log.error("安全审计日志持久化失败: {}", e.getMessage());
        }
    }

    /**
     * 获取客户端IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
