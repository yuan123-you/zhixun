package com.zhixun.controller;

import com.zhixun.common.result.PageResult;
import com.zhixun.common.result.R;
import com.zhixun.common.util.SecurityUtil;
import com.zhixun.service.NotificationService;
import com.zhixun.vo.NotificationVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 通知控制器
 */
@RestController
@RequestMapping("/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final SecurityUtil securityUtil;

    /**
     * 通知列表
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public R<PageResult<NotificationVO>> getNotifications(
            @RequestParam(required = false) Integer type,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Long userId = securityUtil.getCurrentUserId();
        return R.ok(notificationService.getNotifications(userId, type, page, pageSize));
    }

    /**
     * 标记已读
     */
    @PutMapping("/{id}/read")
    @PreAuthorize("isAuthenticated()")
    public R<Void> markAsRead(@PathVariable Long id) {
        Long userId = securityUtil.getCurrentUserId();
        notificationService.markAsRead(userId, id);
        return R.ok();
    }

    /**
     * 全部已读
     */
    @PutMapping("/read-all")
    @PreAuthorize("isAuthenticated()")
    public R<Void> markAllAsRead() {
        Long userId = securityUtil.getCurrentUserId();
        notificationService.markAllAsRead(userId);
        return R.ok();
    }

    /**
     * 未读通知数
     */
    @GetMapping("/unread-count")
    @PreAuthorize("isAuthenticated()")
    public R<Map<String, Object>> getUnreadCount() {
        Long userId = securityUtil.getCurrentUserId();
        Integer count = notificationService.getUnreadCount(userId);
        return R.ok(Map.of("unread_count", count));
    }
}
