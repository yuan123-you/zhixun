package com.zhixun.common.util;

import com.zhixun.common.exception.BusinessException;
import com.zhixun.common.result.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * 安全上下文工具类
 * 用于获取当前登录用户信息
 */
@Slf4j
@Component
public class SecurityUtil {

    /** 角色前缀 */
    private static final String ROLE_PREFIX = "ROLE_";

    /** 管理员角色 */
    private static final String ROLE_ADMIN = "ROLE_ADMIN";

    /**
     * 获取当前认证信息
     *
     * @return 认证信息
     */
    private Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    /**
     * 获取当前登录用户ID
     *
     * @return 用户ID
     */
    public Long getCurrentUserId() {
        Authentication authentication = getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        Object details = authentication.getDetails();
        if (details instanceof Long) {
            return (Long) details;
        }
        if (details instanceof Integer) {
            return ((Integer) details).longValue();
        }
        throw new BusinessException(ErrorCode.UNAUTHORIZED);
    }

    /**
     * 获取当前登录用户名
     *
     * @return 用户名
     */
    public String getCurrentUsername() {
        Authentication authentication = getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof String) {
            return (String) principal;
        }
        throw new BusinessException(ErrorCode.UNAUTHORIZED);
    }

    /**
     * 判断当前用户是否为管理员
     *
     * @return true-是管理员
     */
    public boolean isAdmin() {
        Authentication authentication = getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(auth -> ROLE_ADMIN.equals(auth.getAuthority()));
    }

    /**
     * 检查是否为指定用户或管理员
     *
     * @param userId 目标用户ID
     * @return true-有权限
     */
    public boolean isOwnerOrAdmin(Long userId) {
        if (isAdmin()) {
            return true;
        }
        Long currentUserId = getCurrentUserId();
        return currentUserId.equals(userId);
    }
}
