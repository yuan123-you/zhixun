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
import java.util.Map;

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

    /**
     * 管理端话题列表（包含所有状态的话题）
     */
    @GetMapping("/admin/list")
    @PreAuthorize("hasRole('ADMIN')")
    public R<PageResult<TopicVO>> adminList(TopicQueryRequest request) {
        Page<TopicVO> page = topicService.getAdminTopicList(request);
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

    /**
     * 更新话题状态（显示/隐藏）
     */
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public R<Void> updateStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        Integer status = body.get("status");
        if (status == null || (status != 0 && status != 1)) {
            return R.fail(400, "状态值无效，必须为0（正常）或1（隐藏）");
        }
        topicService.updateTopicStatus(id, status);
        return R.ok();
    }

    /**
     * 删除话题（管理员）
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public R<Void> delete(@PathVariable Long id) {
        topicService.deleteTopic(id);
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