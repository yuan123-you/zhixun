package com.zhixun.controller;

import com.zhixun.common.result.R;
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
     * 创建标签（需管理员）
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public R<Long> create(@Valid @RequestBody TagRequest request) {
        return R.ok(tagService.create(request.getName()));
    }

    /**
     * 更新标签（需管理员）
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody TagRequest request) {
        tagService.update(id, request.getName());
        return R.ok();
    }

    /**
     * 删除标签（需管理员）
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public R<Void> delete(@PathVariable Long id) {
        tagService.delete(id);
        return R.ok();
    }
}
