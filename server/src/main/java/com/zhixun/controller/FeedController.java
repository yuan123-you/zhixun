package com.zhixun.controller;

import com.zhixun.common.result.PageResult;
import com.zhixun.common.result.R;
import com.zhixun.common.util.SecurityUtil;
import com.zhixun.service.FeedService;
import com.zhixun.vo.ArticleVO;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

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
    public ResponseEntity<R<PageResult<ArticleVO>>> recommend(
            @RequestParam(defaultValue = "0") Integer refresh,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            HttpServletResponse response) {
        // 尝试获取当前登录用户ID（未登录为 null）
        Long userId = null;
        try {
            userId = securityUtil.getCurrentUserId();
        } catch (Exception e) {
            // 未登录用户
        }
        // 刷新时禁用缓存，确保每次刷新都拿到全新的推荐结果
        if (refresh != null && refresh == 1) {
            response.setHeader("Cache-Control", "no-cache, no-store");
        } else {
            response.setHeader("Cache-Control", "public, max-age=60");
        }
        return ResponseEntity.ok(R.ok(feedService.getRecommendFeed(userId, refresh, page, pageSize)));
    }

    /**
     * 最新发布
     */
    @GetMapping("/latest")
    public ResponseEntity<R<PageResult<ArticleVO>>> latest(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            HttpServletResponse response) {
        // 设置 Cache-Control：公共缓存，最大缓存60秒
        response.setHeader("Cache-Control", "public, max-age=60");
        return ResponseEntity.ok(R.ok(feedService.getLatestFeed(page, pageSize)));
    }

    /**
     * 关注动态（页码分页）
     */
    @GetMapping("/following")
    public ResponseEntity<R<PageResult<ArticleVO>>> following(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            HttpServletResponse response) {
        Long userId = securityUtil.getCurrentUserId();
        // 关注动态为个人数据，设置 no-cache
        response.setHeader("Cache-Control", "no-cache");
        return ResponseEntity.ok(R.ok(feedService.getFollowingFeed(userId, page, pageSize)));
    }

    /**
     * 关注动态（游标分页，支持无限滚动）
     */
    @GetMapping("/following/cursor")
    public ResponseEntity<R<PageResult<ArticleVO>>> followingByCursor(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime cursor,
            @RequestParam(defaultValue = "10") Integer pageSize,
            HttpServletResponse response) {
        Long userId = securityUtil.getCurrentUserId();
        // 关注动态为个人数据，设置 no-cache
        response.setHeader("Cache-Control", "no-cache");
        return ResponseEntity.ok(R.ok(feedService.getFollowingFeedByCursor(userId, cursor, pageSize)));
    }

    /**
     * 热门推荐
     */
    @GetMapping("/hot")
    public ResponseEntity<R<PageResult<ArticleVO>>> hot(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            HttpServletResponse response) {
        // 设置 Cache-Control：公共缓存，最大缓存120秒
        response.setHeader("Cache-Control", "public, max-age=120");
        return ResponseEntity.ok(R.ok(feedService.getHotFeed(page, pageSize)));
    }
}
