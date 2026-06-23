package com.zhixun.controller;

import com.zhixun.common.annotation.OperationLog;
import com.zhixun.common.result.R;
import com.zhixun.common.util.SecurityUtil;
import com.zhixun.dto.article.TagMergeRequest;
import com.zhixun.dto.article.TagRequest;
import com.zhixun.entity.Tag;
import com.zhixun.service.TagService;
import com.zhixun.vo.TagVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 标签控制器
 */
@RestController
@RequestMapping("/v1/tags")
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;
    private final SecurityUtil securityUtil;

    /**
     * 标签列表（公开）
     */
    @GetMapping
    public R<List<TagVO>> list() {
        return R.ok(tagService.list());
    }

    /**
     * 热门标签（公开）
     */
    @GetMapping("/hot")
    public R<List<TagVO>> hot(@RequestParam(defaultValue = "10") int limit) {
        return R.ok(tagService.hot(limit));
    }

    /**
     * 标签云（公开）
     */
    @GetMapping("/cloud")
    public R<List<TagVO>> cloud() {
        return R.ok(tagService.getTagCloud());
    }

    /**
     * 搜索标签（公开，用于自动补全）
     */
    @GetMapping("/search")
    public R<List<TagVO>> search(@RequestParam String keyword) {
        return R.ok(tagService.searchTags(keyword));
    }

    /**
     * 获取当前用户关注的标签
     */
    @GetMapping("/followed")
    public R<List<TagVO>> followed() {
        Long userId = securityUtil.getCurrentUserId();
        return R.ok(tagService.getFollowedTags(userId));
    }

    /**
     * 创建标签（需管理员）
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @OperationLog(module = "标签", action = "创建")
    public R<Long> create(@Valid @RequestBody TagRequest request) {
        return R.ok(tagService.create(request.getName()));
    }

    /**
     * 关注标签
     */
    @PostMapping("/{id}/follow")
    public R<Void> followTag(@PathVariable Long id) {
        Long userId = securityUtil.getCurrentUserId();
        tagService.followTag(userId, id);
        return R.ok();
    }

    /**
     * 取消关注标签
     */
    @DeleteMapping("/{id}/follow")
    public R<Void> unfollowTag(@PathVariable Long id) {
        Long userId = securityUtil.getCurrentUserId();
        tagService.unfollowTag(userId, id);
        return R.ok();
    }

    /**
     * 更新标签（需管理员）
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @OperationLog(module = "标签", action = "更新")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody TagRequest request) {
        tagService.update(id, request.getName());
        return R.ok();
    }

    /**
     * 删除标签（需管理员）
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @OperationLog(module = "标签", action = "删除")
    public R<Void> delete(@PathVariable Long id) {
        tagService.delete(id);
        return R.ok();
    }

    /**
     * 合并标签（需管理员）
     */
    @PostMapping("/merge")
    @PreAuthorize("hasRole('ADMIN')")
    @OperationLog(module = "标签", action = "合并")
    public R<Void> merge(@Valid @RequestBody TagMergeRequest request) {
        tagService.mergeTag(request.getSourceTagId(), request.getTargetTagId());
        return R.ok();
    }

    /**
     * 同步标签文章数（需管理员）
     */
    @PostMapping("/sync-article-count")
    @PreAuthorize("hasRole('ADMIN')")
    public R<Void> syncArticleCount(@RequestParam(required = false) Long tagId) {
        tagService.syncTagArticleCount(tagId);
        return R.ok();
    }
}
