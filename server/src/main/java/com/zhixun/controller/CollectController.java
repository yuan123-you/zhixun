package com.zhixun.controller;

import com.zhixun.common.annotation.OperationLog;
import com.zhixun.common.result.PageResult;
import com.zhixun.common.result.R;
import com.zhixun.common.util.SecurityUtil;
import com.zhixun.service.CollectService;
import com.zhixun.vo.ArticleVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 收藏控制器
 */
@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
public class CollectController {

    private final CollectService collectService;
    private final SecurityUtil securityUtil;

    /**
     * 收藏/取消收藏文章
     */
    @PostMapping("/articles/{id}/collect")
    @PreAuthorize("isAuthenticated()")
    @OperationLog(module = "收藏", action = "收藏/取消收藏")
    public R<Map<String, Object>> toggleCollect(@PathVariable Long id,
                                                 @RequestParam(required = false) String groupName) {
        Long userId = securityUtil.getCurrentUserId();
        Map<String, Object> result = collectService.toggleCollect(userId, id, groupName);
        return R.ok(result);
    }

    /**
     * 我的收藏列表
     */
    @GetMapping("/user/collects")
    @PreAuthorize("isAuthenticated()")
    public R<PageResult<ArticleVO>> getUserCollects(
            @RequestParam(required = false) String groupName,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Long userId = securityUtil.getCurrentUserId();
        return R.ok(collectService.getUserCollects(userId, groupName, page, pageSize));
    }

    /**
     * 收藏夹分组列表
     */
    @GetMapping("/user/collect-groups")
    @PreAuthorize("isAuthenticated()")
    public R<List<String>> getCollectGroups() {
        Long userId = securityUtil.getCurrentUserId();
        return R.ok(collectService.getCollectGroups(userId));
    }
}
