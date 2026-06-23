package com.zhixun.controller;

import com.zhixun.common.annotation.OperationLog;
import com.zhixun.common.result.PageResult;
import com.zhixun.common.result.R;
import com.zhixun.common.util.SecurityUtil;
import com.zhixun.dto.user.ProfileUpdateRequest;
import com.zhixun.dto.user.SettingsUpdateRequest;
import com.zhixun.service.CommentService;
import com.zhixun.service.OnlineStatusService;
import com.zhixun.service.UserService;
import com.zhixun.vo.ArticleVO;
import com.zhixun.vo.CommentVO;
import com.zhixun.vo.UserSettingsVO;
import com.zhixun.vo.UserVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 用户控制器
 */
@RestController
@RequestMapping("/v1/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final CommentService commentService;
    private final OnlineStatusService onlineStatusService;
    private final SecurityUtil securityUtil;

    /**
     * 获取个人资料
     */
    @GetMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public R<UserVO> getProfile() {
        Long userId = securityUtil.getCurrentUserId();
        return R.ok(userService.getProfile(userId));
    }

    /**
     * 更新个人资料
     */
    @PutMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    @OperationLog(module = "用户设置", action = "修改个人资料")
    public R<Void> updateProfile(@Valid @RequestBody ProfileUpdateRequest request) {
        Long userId = securityUtil.getCurrentUserId();
        userService.updateProfile(userId, request);
        return R.ok();
    }

    /**
     * 我发布的文章
     */
    @GetMapping("/articles")
    @PreAuthorize("isAuthenticated()")
    public R<PageResult<ArticleVO>> getUserArticles(
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Long userId = securityUtil.getCurrentUserId();
        return R.ok(userService.getUserArticles(userId, status, page, pageSize));
    }

    /**
     * 我的点赞
     */
    @GetMapping("/likes")
    @PreAuthorize("isAuthenticated()")
    public R<PageResult<ArticleVO>> getUserLikes(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Long userId = securityUtil.getCurrentUserId();
        return R.ok(userService.getUserLikes(userId, page, pageSize));
    }

    /**
     * 我的评论
     */
    @GetMapping("/comments")
    @PreAuthorize("isAuthenticated()")
    public R<PageResult<CommentVO>> getUserComments(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Long userId = securityUtil.getCurrentUserId();
        return R.ok(commentService.getMyComments(userId, page, pageSize));
    }

    /**
     * 浏览历史
     */
    @GetMapping("/view-history")
    @PreAuthorize("isAuthenticated()")
    public R<PageResult<ArticleVO>> getViewHistory(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Long userId = securityUtil.getCurrentUserId();
        return R.ok(userService.getViewHistory(userId, startDate, endDate, page, pageSize));
    }

    /**
     * 获取全局设置
     */
    @GetMapping("/settings")
    @PreAuthorize("isAuthenticated()")
    public R<UserSettingsVO> getSettings() {
        Long userId = securityUtil.getCurrentUserId();
        return R.ok(userService.getSettings(userId));
    }

    /**
     * 更新全局设置
     */
    @PutMapping("/settings")
    @PreAuthorize("isAuthenticated()")
    @OperationLog(module = "用户设置", action = "修改全局设置")
    public R<Void> updateSettings(@Valid @RequestBody SettingsUpdateRequest request) {
        Long userId = securityUtil.getCurrentUserId();
        userService.updateSettings(userId, request);
        return R.ok();
    }

    /**
     * 更新在线状态可见性
     * 专用端点，修改后自动清除缓存
     */
    @PutMapping("/online-status-visibility")
    @PreAuthorize("isAuthenticated()")
    @OperationLog(module = "用户设置", action = "修改在线状态可见性")
    public R<Void> updateOnlineStatusVisibility(@RequestBody Map<String, Integer> body) {
        Long userId = securityUtil.getCurrentUserId();
        Integer showOnlineStatus = body.get("show_online_status");
        if (showOnlineStatus == null || (showOnlineStatus != 0 && showOnlineStatus != 1)) {
            return R.fail(400, "show_online_status 参数错误，仅支持 0 或 1");
        }
        onlineStatusService.updateShowOnlineStatus(userId, showOnlineStatus);
        return R.ok();
    }

    /**
     * 批量获取用户在线状态
     * 用于关注/粉丝列表等场景
     */
    @GetMapping("/online-status/batch")
    @PreAuthorize("isAuthenticated()")
    public R<Map<Long, Boolean>> batchGetOnlineStatus(@RequestParam List<Long> userIds) {
        Long requesterId = securityUtil.getCurrentUserId();
        Map<Long, Boolean> result = onlineStatusService.batchGetOnlineStatus(userIds, requesterId);
        return R.ok(result);
    }
}
