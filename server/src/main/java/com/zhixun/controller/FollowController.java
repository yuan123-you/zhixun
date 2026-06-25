package com.zhixun.controller;

import com.zhixun.common.annotation.OperationLog;
import com.zhixun.common.result.PageResult;
import com.zhixun.common.result.R;
import com.zhixun.common.util.SecurityUtil;
import com.zhixun.entity.User;
import com.zhixun.mapper.UserMapper;
import com.zhixun.service.FollowService;
import com.zhixun.service.OnlineStatusService;
import com.zhixun.service.UserService;
import com.zhixun.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * 关注控制器
 */
@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
public class FollowController {

    private final FollowService followService;
    private final OnlineStatusService onlineStatusService;
    private final UserMapper userMapper;
    private final UserService userService;
    private final SecurityUtil securityUtil;

    /**
     * 获取当前登录用户信息
     */
    @GetMapping("/users/me")
    @PreAuthorize("isAuthenticated()")
    public R<UserVO> getCurrentUser() {
        Long userId = securityUtil.getCurrentUserId();
        return R.ok(userService.getProfile(userId));
    }

    /**
     * 获取指定用户信息
     */
    @GetMapping("/users/{id}")
    public R<UserVO> getUserById(@PathVariable Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            return R.fail(404, "用户不存在");
        }
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUid(user.getUid());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setBio(user.getBio());
        vo.setProvince(user.getProvince());
        vo.setRole(user.getRole() != null ? user.getRole().name() : null);
        vo.setStatus(user.getStatus());
        vo.setCreatedAt(user.getCreatedAt());
        vo.setFollowCount(user.getFollowCount());
        vo.setFollowerCount(user.getFollowerCount());
        vo.setArticleCount(user.getArticleCount());

        // 如果当前用户已登录，查询是否关注了该用户
        try {
            Long currentUserId = securityUtil.getCurrentUserId();
            if (currentUserId != null && !currentUserId.equals(id)) {
                vo.setIsFollowing(followService.isFollowed(currentUserId, id));
            }
        } catch (Exception e) {
            // 未登录用户，不设置 isFollowing
        }

        return R.ok(vo);
    }

    /**
     * 关注/取消关注用户
     */
    @PostMapping("/users/{id}/follow")
    @PreAuthorize("isAuthenticated()")
    @OperationLog(module = "关注", action = "关注/取消关注")
    public R<Map<String, Object>> toggleFollow(@PathVariable Long id) {
        Long userId = securityUtil.getCurrentUserId();
        Map<String, Object> result = followService.toggleFollow(userId, id);
        return R.ok(result);
    }

    /**
     * 获取用户的关注列表
     */
    @GetMapping("/users/{id}/following")
    public R<PageResult<UserVO>> getFollowing(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword) {
        return R.ok(followService.getFollowing(id, page, pageSize, keyword));
    }

    /**
     * 获取用户的粉丝列表
     */
    @GetMapping("/users/{id}/followers")
    public R<PageResult<UserVO>> getFollowers(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword) {
        return R.ok(followService.getFollowers(id, page, pageSize, keyword));
    }

    /**
     * 获取用户在线状态
     */
    @GetMapping("/users/{id}/online-status")
    @PreAuthorize("isAuthenticated()")
    public R<Map<String, Object>> getOnlineStatus(@PathVariable Long id) {
        Long requesterId = securityUtil.getCurrentUserId();
        Boolean online = onlineStatusService.getOnlineStatus(id, requesterId);

        User user = userMapper.selectById(id);
        String lastActiveAt = null;
        if (user != null && user.getLastActiveAt() != null) {
            lastActiveAt = user.getLastActiveAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }

        // online 为 null 表示目标用户隐藏了在线状态
        if (online == null) {
            return R.ok(Map.of(
                    "is_online", 0,
                    "show_online_status", 0,
                    "last_active_at", ""
            ));
        }

        return R.ok(Map.of(
                "is_online", online ? 1 : 0,
                "show_online_status", 1,
                "last_active_at", lastActiveAt != null ? lastActiveAt : ""
        ));
    }
}
