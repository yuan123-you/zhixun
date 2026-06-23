package com.zhixun.controller;

import com.zhixun.common.annotation.OperationLog;
import com.zhixun.common.result.R;
import com.zhixun.common.util.SecurityUtil;
import com.zhixun.service.LikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 点赞控制器
 */
@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
public class LikeController {

    private final LikeService likeService;
    private final SecurityUtil securityUtil;

    /**
     * 点赞/取消点赞文章
     */
    @PostMapping("/articles/{id}/like")
    @PreAuthorize("isAuthenticated()")
    @OperationLog(module = "点赞", action = "点赞/取消点赞文章")
    public R<Map<String, Object>> toggleArticleLike(@PathVariable Long id) {
        Long userId = securityUtil.getCurrentUserId();
        Map<String, Object> result = likeService.toggleLike(userId, id, 1);
        return R.ok(result);
    }

    /**
     * 点赞/取消点赞评论
     */
    @PostMapping("/comments/{id}/like")
    @PreAuthorize("isAuthenticated()")
    @OperationLog(module = "点赞", action = "点赞/取消点赞评论")
    public R<Map<String, Object>> toggleCommentLike(@PathVariable Long id) {
        Long userId = securityUtil.getCurrentUserId();
        Map<String, Object> result = likeService.toggleLike(userId, id, 2);
        return R.ok(result);
    }
}
