package com.zhixun.controller;

import com.zhixun.common.result.PageResult;
import com.zhixun.common.result.R;
import com.zhixun.common.util.SecurityUtil;
import com.zhixun.service.FeedService;
import com.zhixun.vo.ArticleVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 信息流控制器
 */
@RestController
@RequestMapping("/v1/feed")
@RequiredArgsConstructor
public class FeedController {

    private final FeedService feedService;
    private final SecurityUtil securityUtil;

    /**
     * 首页个性化推荐
     */
    @GetMapping("/recommend")
    public R<PageResult<ArticleVO>> recommend(
            @RequestParam(defaultValue = "0") Integer refresh,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        // 尝试获取当前登录用户ID（未登录为 null）
        Long userId = null;
        try {
            userId = securityUtil.getCurrentUserId();
        } catch (Exception e) {
            // 未登录用户
        }
        return R.ok(feedService.getRecommendFeed(userId, refresh, page, pageSize));
    }

    /**
     * 最新发布
     */
    @GetMapping("/latest")
    public R<PageResult<ArticleVO>> latest(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return R.ok(feedService.getLatestFeed(page, pageSize));
    }

    /**
     * 关注动态
     */
    @GetMapping("/following")
    public R<PageResult<ArticleVO>> following(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Long userId = securityUtil.getCurrentUserId();
        return R.ok(feedService.getFollowingFeed(userId, page, pageSize));
    }
}
