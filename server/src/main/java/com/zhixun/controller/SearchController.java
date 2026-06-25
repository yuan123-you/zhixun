package com.zhixun.controller;

import com.zhixun.common.result.R;
import com.zhixun.common.util.SecurityUtil;
import com.zhixun.service.SearchService;
import com.zhixun.vo.SearchResultVO;
import com.zhixun.vo.SearchSuggestResultVO;
import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 搜索控制器
 */
@RestController
@RequestMapping("/v1/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;
    private final SecurityUtil securityUtil;

    /**
     * 综合搜索
     */
    @GetMapping
    @SentinelResource(value = "search", blockHandler = "searchBlockHandler", blockHandlerClass = SearchController.BlockHandlers.class, fallback = "searchFallback")
    public R<SearchResultVO> search(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "all") String type,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long tagId,
            @RequestParam(required = false) String timeRange,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "default") String sort,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return R.ok(searchService.search(keyword, type, categoryId, tagId,
                timeRange, startDate, endDate, sort, page, pageSize));
    }

    /**
     * 搜索建议
     */
    @GetMapping("/suggest")
    public R<SearchSuggestResultVO> suggest(@RequestParam String keyword) {
        return R.ok(searchService.getSuggestions(keyword));
    }

    /**
     * 热门搜索词
     */
    @GetMapping("/hot")
    public R<List<String>> hotSearches() {
        return R.ok(searchService.getHotSearches());
    }

    /**
     * 清空搜索历史
     * 防御性编程：显式添加 @PreAuthorize 确保只有已认证用户可调用
     */
    @DeleteMapping("/history")
    @PreAuthorize("isAuthenticated()")
    public R<Void> clearHistory() {
        Long userId = securityUtil.getCurrentUserId();
        searchService.clearSearchHistory(userId);
        return R.ok();
    }

    /**
     * Sentinel 限流降级处理
     */
    public static class BlockHandlers {
        public static R<SearchResultVO> searchBlockHandler(String keyword, String type, Long categoryId,
                Long tagId, String timeRange, String startDate, String endDate,
                String sort, Integer page, Integer pageSize, BlockException e) {
            return R.fail(429, "搜索请求过于频繁，请稍后重试");
        }
    }

    /**
     * 搜索服务降级处理（异常时返回空结果）
     */
    public R<SearchResultVO> searchFallback(String keyword, String type, Long categoryId,
            Long tagId, String timeRange, String startDate, String endDate,
            String sort, Integer page, Integer pageSize, Throwable t) {
        return R.fail(503, "搜索服务暂时不可用，请稍后重试");
    }
}
