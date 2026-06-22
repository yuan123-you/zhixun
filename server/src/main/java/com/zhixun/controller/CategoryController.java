package com.zhixun.controller;

import com.zhixun.common.result.R;
import com.zhixun.dto.article.CategoryRequest;
import com.zhixun.entity.Category;
import com.zhixun.service.CategoryService;
import com.zhixun.vo.CategoryVO;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 分类控制器
 */
@RestController
@RequestMapping("/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    /**
     * 分类列表（公开）
     */
    @GetMapping
    public R<List<Category>> list() {
        return R.ok(categoryService.list());
    }

    /**
     * 分类树（公开）
     */
    @GetMapping("/tree")
    public R<List<CategoryVO>> tree() {
        return R.ok(categoryService.tree());
    }

    /**
     * 创建分类（需管理员）
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public R<Long> create(@Valid @RequestBody CategoryRequest request) {
        return R.ok(categoryService.create(
                request.getName(),
                request.getParentId(),
                request.getSort(),
                request.getIcon()));
    }

    /**
     * 更新分类（需管理员）
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
        categoryService.update(
                id,
                request.getName(),
                request.getParentId(),
                request.getSort(),
                request.getIcon());
        return R.ok();
    }

    /**
     * 删除分类（需管理员）
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public R<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return R.ok();
    }
}
