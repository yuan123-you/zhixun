package com.zhixun.controller;

import com.zhixun.common.result.PageResult;
import com.zhixun.common.result.R;
import com.zhixun.common.util.SecurityUtil;
import com.zhixun.dto.topic.TopicCreateRequest;
import com.zhixun.dto.topic.TopicQueryRequest;
import com.zhixun.service.TopicService;
import com.zhixun.vo.TopicVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/v1/topics")
@RequiredArgsConstructor
public class TopicController {
    private final TopicService topicService;
    private final SecurityUtil securityUtil;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public R<Long> create(@Valid @RequestBody TopicCreateRequest request) {
        return R.ok(topicService.createTopic(securityUtil.getCurrentUserId(), request));
    }

    @GetMapping
    public R<PageResult<TopicVO>> list(TopicQueryRequest request) {
        Page<TopicVO> page = topicService.getTopicList(request);
        return R.ok(new PageResult<>(page.getRecords(), page.getTotal(), request.getPage(), request.getPageSize()));
    }

    @GetMapping("/{id}")
    public R<TopicVO> detail(@PathVariable Long id) {
        Long uid = null;
        try { uid = securityUtil.getCurrentUserId(); } catch (Exception e) {}
        return R.ok(topicService.getTopicDetail(id, uid));
    }

    @PostMapping("/{id}/follow")
    @PreAuthorize("isAuthenticated()")
    public R<Void> toggleFollow(@PathVariable Long id) {
        topicService.toggleFollow(securityUtil.getCurrentUserId(), id);
        return R.ok();
    }

    @GetMapping("/hot")
    public R<List<TopicVO>> hot(@RequestParam(defaultValue = "20") int limit) {
        return R.ok(topicService.getHotTopics(limit));
    }

    @GetMapping("/search")
    public R<List<TopicVO>> search(@RequestParam String keyword, @RequestParam(defaultValue = "10") int limit) {
        return R.ok(topicService.searchTopics(keyword, limit));
    }

    @GetMapping("/followed")
    @PreAuthorize("isAuthenticated()")
    public R<List<TopicVO>> followed() {
        return R.ok(topicService.getUserFollowedTopics(securityUtil.getCurrentUserId()));
    }
}