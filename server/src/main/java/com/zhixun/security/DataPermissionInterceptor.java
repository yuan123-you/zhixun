package com.zhixun.security;

import com.zhixun.common.util.SecurityUtil;
import com.zhixun.entity.Article;
import com.zhixun.entity.Collect;
import com.zhixun.entity.Comment;
import com.zhixun.entity.UserMessage;
import com.zhixun.mapper.ArticleMapper;
import com.zhixun.mapper.CollectMapper;
import com.zhixun.mapper.CommentMapper;
import com.zhixun.mapper.UserMessageMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhixun.common.result.R;
import com.zhixun.common.result.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * 数据权限拦截器
 * 防止水平越权（用户操作他人数据）和垂直越权（普通用户访问管理功能）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataPermissionInterceptor implements HandlerInterceptor {

    private final ArticleMapper articleMapper;
    private final CommentMapper commentMapper;
    private final UserMessageMapper userMessageMapper;
    private final CollectMapper collectMapper;
    private final SecurityUtil securityUtil;
    private final SecurityAuditLogger securityAuditLogger;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate stringRedisTemplate;

    /** 越权失败次数 Redis Key 前缀 */
    private static final String AUTH_FAIL_KEY_PREFIX = "auth:fail:";
    /** 最大失败次数 */
    private static final int MAX_FAIL_COUNT = 5;
    /** 失败计数窗口（分钟） */
    private static final long FAIL_WINDOW_MINUTES = 10;
    /** 封锁时长（分钟） */
    private static final long BLOCK_DURATION_MINUTES = 10;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();
        String method = request.getMethod();

        try {
            // 检查是否被频率限制封锁
            if (isBlocked(request)) {
                securityAuditLogger.logPermissionDenied(request, "频率限制封锁", "IP已被临时封锁");
                sendTooManyRequests(response, "操作过于频繁，请稍后再试");
                return false;
            }

            // 作品编辑/删除权限校验
            if (isArticleModify(uri, method)) {
                Long articleId = extractIdFromPath(uri, "/v1/articles/");
                if (articleId != null && !checkArticlePermission(articleId)) {
                    handlePermissionDenied(request, response, "作品操作越权", "articleId=" + articleId, "无权操作该作品");
                    return false;
                }
            }

            // 评论删除权限校验
            if (isCommentDelete(uri, method)) {
                Long commentId = extractIdFromPath(uri, "/v1/comments/");
                if (commentId != null && !checkCommentPermission(commentId)) {
                    handlePermissionDenied(request, response, "评论操作越权", "commentId=" + commentId, "无权删除该评论");
                    return false;
                }
            }

            // 私信读取权限校验
            if (isMessageRead(uri, method)) {
                Long targetUserId = extractIdFromPath(uri, "/v1/messages/");
                if (targetUserId != null && !checkMessagePermission(targetUserId)) {
                    handlePermissionDenied(request, response, "私信读取越权", "targetUserId=" + targetUserId, "无权读取该会话");
                    return false;
                }
            }

            // 收藏操作权限校验（删除收藏）
            if (isCollectDelete(uri, method)) {
                Long collectId = extractIdFromPath(uri, "/v1/collects/");
                if (collectId != null && !checkCollectPermission(collectId)) {
                    handlePermissionDenied(request, response, "收藏操作越权", "collectId=" + collectId, "无权操作该收藏");
                    return false;
                }
            }

            // 用户资料更新权限校验
            if (isProfileUpdate(uri, method)) {
                Long targetUserId = extractUserIdFromPath(uri);
                if (targetUserId != null && !checkUserSelfPermission(targetUserId)) {
                    handlePermissionDenied(request, response, "资料更新越权", "targetUserId=" + targetUserId, "无权修改他人资料");
                    return false;
                }
            }

            // 用户设置更新权限校验
            if (isSettingsUpdate(uri, method)) {
                Long targetUserId = extractUserIdFromPath(uri);
                if (targetUserId != null && !checkUserSelfPermission(targetUserId)) {
                    handlePermissionDenied(request, response, "设置更新越权", "targetUserId=" + targetUserId, "无权修改他人设置");
                    return false;
                }
            }

        } catch (Exception e) {
            // 未认证用户由Spring Security处理，此处不拦截
            log.debug("数据权限校验异常（可能未认证）: {}", e.getMessage());
        }

        return true;
    }

    /**
     * 处理权限拒绝：记录审计日志 + 增加失败计数
     */
    private void handlePermissionDenied(HttpServletRequest request, HttpServletResponse response,
                                        String eventType, String detail, String message) throws IOException {
        securityAuditLogger.logPermissionDenied(request, eventType, detail);
        incrementFailCount(request);
        sendForbidden(response, message);
    }

    // ========== 频率限制方法 ==========

    /**
     * 检查IP是否被封锁
     */
    private boolean isBlocked(HttpServletRequest request) {
        String key = AUTH_FAIL_KEY_PREFIX + getClientIp(request);
        String countStr = stringRedisTemplate.opsForValue().get(key);
        if (countStr != null && Integer.parseInt(countStr) >= MAX_FAIL_COUNT) {
            return true;
        }
        return false;
    }

    /**
     * 增加失败计数
     */
    private void incrementFailCount(HttpServletRequest request) {
        String key = AUTH_FAIL_KEY_PREFIX + getClientIp(request);
        Long count = stringRedisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            // 首次失败，设置过期时间
            stringRedisTemplate.expire(key, FAIL_WINDOW_MINUTES, TimeUnit.MINUTES);
        }
        // 达到最大失败次数时，延长封锁时间
        if (count != null && count == MAX_FAIL_COUNT) {
            stringRedisTemplate.expire(key, BLOCK_DURATION_MINUTES, TimeUnit.MINUTES);
            log.warn("IP {} 连续 {} 次越权失败，已临时封锁 {} 分钟", getClientIp(request), MAX_FAIL_COUNT, BLOCK_DURATION_MINUTES);
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

    // ========== URL匹配方法 ==========

    /**
     * 判断是否为作品编辑/删除请求
     */
    private boolean isArticleModify(String uri, String method) {
        return ("PUT".equals(method) || "DELETE".equals(method))
                && uri.matches("/v1/articles/\\d+.*");
    }

    /**
     * 判断是否为评论删除请求
     */
    private boolean isCommentDelete(String uri, String method) {
        return "DELETE".equals(method) && uri.matches("/v1/comments/\\d+");
    }

    /**
     * 判断是否为私信读取请求
     */
    private boolean isMessageRead(String uri, String method) {
        return "GET".equals(method) && uri.matches("/v1/messages/\\d+");
    }

    /**
     * 判断是否为收藏删除请求
     */
    private boolean isCollectDelete(String uri, String method) {
        return "DELETE".equals(method) && uri.matches("/v1/collects/\\d+");
    }

    /**
     * 判断是否为用户资料更新请求
     * 匹配 PUT /v1/users/{id}/profile 或 PUT /v1/user/{id}/profile
     */
    private boolean isProfileUpdate(String uri, String method) {
        return "PUT".equals(method)
                && (uri.matches("/v1/users/\\d+/profile") || uri.matches("/v1/user/\\d+/profile"));
    }

    /**
     * 判断是否为用户设置更新请求
     * 匹配 PUT /v1/users/{id}/settings 或 PUT /v1/user/{id}/settings
     */
    private boolean isSettingsUpdate(String uri, String method) {
        return ("PUT".equals(method) || "PATCH".equals(method))
                && (uri.matches("/v1/users/\\d+/settings") || uri.matches("/v1/user/\\d+/settings"));
    }

    // ========== 权限校验方法 ==========

    /**
     * 校验作品操作权限：当前用户是作品作者或管理员
     */
    private boolean checkArticlePermission(Long articleId) {
        if (securityUtil.isAdmin()) {
            return true;
        }
        Long currentUserId = securityUtil.getCurrentUserId();
        Article article = articleMapper.selectById(articleId);
        if (article == null) {
            return false;
        }
        return currentUserId.equals(article.getAuthorId());
    }

    /**
     * 校验评论删除权限：当前用户是评论者或管理员
     */
    private boolean checkCommentPermission(Long commentId) {
        if (securityUtil.isAdmin()) {
            return true;
        }
        Long currentUserId = securityUtil.getCurrentUserId();
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            return false;
        }
        return currentUserId.equals(comment.getUserId());
    }

    /**
     * 校验私信读取权限：当前用户是会话参与方
     */
    private boolean checkMessagePermission(Long targetUserId) {
        Long currentUserId = securityUtil.getCurrentUserId();
        Long count = userMessageMapper.selectCount(
                new LambdaQueryWrapper<UserMessage>()
                        .and(w -> w
                                .eq(UserMessage::getSenderId, currentUserId)
                                .eq(UserMessage::getReceiverId, targetUserId)
                        )
                        .or(w -> w
                                .eq(UserMessage::getSenderId, targetUserId)
                                .eq(UserMessage::getReceiverId, currentUserId)
                        )
        );
        return count > 0;
    }

    /**
     * 校验收藏操作权限：当前用户是收藏者或管理员
     */
    private boolean checkCollectPermission(Long collectId) {
        if (securityUtil.isAdmin()) {
            return true;
        }
        Long currentUserId = securityUtil.getCurrentUserId();
        Collect collect = collectMapper.selectById(collectId);
        if (collect == null) {
            return false;
        }
        return currentUserId.equals(collect.getUserId());
    }

    /**
     * 校验用户自身操作权限：当前用户是目标用户或管理员
     */
    private boolean checkUserSelfPermission(Long targetUserId) {
        if (securityUtil.isAdmin()) {
            return true;
        }
        Long currentUserId = securityUtil.getCurrentUserId();
        return currentUserId.equals(targetUserId);
    }

    // ========== 工具方法 ==========

    /**
     * 从URI路径中提取资源ID
     */
    private Long extractIdFromPath(String uri, String prefix) {
        try {
            int start = uri.indexOf(prefix);
            if (start < 0) {
                return null;
            }
            String remaining = uri.substring(start + prefix.length());
            int slashIndex = remaining.indexOf('/');
            String idStr = (slashIndex > 0) ? remaining.substring(0, slashIndex) : remaining;
            return Long.parseLong(idStr);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 从用户相关路径中提取用户ID
     * 匹配 /v1/users/{id}/... 或 /v1/user/{id}/...
     */
    private Long extractUserIdFromPath(String uri) {
        try {
            // 匹配 /v1/users/{id}/ 或 /v1/user/{id}/
            java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("/v1/users?/(\\d+)").matcher(uri);
            if (matcher.find()) {
                return Long.parseLong(matcher.group(1));
            }
        } catch (NumberFormatException e) {
            // 忽略
        }
        return null;
    }

    /**
     * 返回403禁止访问响应
     */
    private void sendForbidden(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        R<Void> result = R.fail(ErrorCode.FORBIDDEN, message);
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }

    /**
     * 返回429请求过多响应
     */
    private void sendTooManyRequests(HttpServletResponse response, String message) throws IOException {
        response.setStatus(429);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        R<Void> result = R.fail(ErrorCode.TOO_MANY_REQUESTS, message);
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }
}
