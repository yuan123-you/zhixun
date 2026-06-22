package com.zhixun.controller;

import com.zhixun.common.result.PageResult;
import com.zhixun.common.result.R;
import com.zhixun.common.util.SecurityUtil;
import com.zhixun.dto.user.ProfileUpdateRequest;
import com.zhixun.dto.user.SettingsUpdateRequest;
import com.zhixun.service.CommentService;
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

/**
 * 用户控制器
 */
@RestController
@RequestMapping("/v1/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final CommentService commentService;
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
    public R<Void> updateSettings(@Valid @RequestBody SettingsUpdateRequest request) {
        Long userId = securityUtil.getCurrentUserId();
        userService.updateSettings(userId, request);
        return R.ok();
    }
}
