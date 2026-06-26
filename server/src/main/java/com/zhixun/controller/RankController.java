package com.zhixun.controller;

import com.zhixun.common.result.R;
import com.zhixun.service.RankService;
import com.zhixun.service.SearchHistoryService;
import com.zhixun.vo.HotArticleVO;
import com.zhixun.vo.TagVO;
import com.zhixun.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 排行控制器
 */
@RestController
@RequestMapping("/v1/rank")
@RequiredArgsConstructor
public class RankController {

    private final RankService rankService;
    private final SearchHistoryService searchHistoryService;

    /**
     * 热点排行榜
     */
    @GetMapping("/hot")
    public R<List<HotArticleVO>> hotRank(
            @RequestParam(defaultValue = "daily") String period,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "10") Integer limit) {
        return R.ok(rankService.getHotRank(period, categoryId, limit));
    }

    /**
     * 热门标签（最近7天作品数最多的标签）
     */
    @GetMapping("/hot-tags")
    public R<List<TagVO>> hotTags(
            @RequestParam(defaultValue = "10") Integer limit) {
        return R.ok(rankService.getHotTags(limit));
    }

    /**
     * 热门用户（最近7天作品浏览量最高的用户）
     */
    @GetMapping("/hot-users")
    public R<List<UserVO>> hotUsers(
            @RequestParam(defaultValue = "10") Integer limit) {
        return R.ok(rankService.getHotUsers(limit));
    }

    /**
     * 实时热榜（基于 Redis Sorted Set 滑动窗口）
     */
    @GetMapping("/realtime")
    public R<List<HotArticleVO>> realtime(
            @RequestParam(defaultValue = "20") Integer limit) {
        return R.ok(rankService.getRealtimeHot(limit));
    }

    /**
     * 热门搜索词（公开接口）
     */
    @GetMapping("/hot-search")
    public R<List<String>> hotSearch(
            @RequestParam(defaultValue = "10") Integer limit) {
        return R.ok(searchHistoryService.getHotSearchKeywords(limit));
    }
}
