package com.zhixun.controller;

import com.zhixun.common.result.R;
import com.zhixun.dto.banner.BannerRequest;
import com.zhixun.service.BannerService;
import com.zhixun.vo.BannerVO;
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
 * 轮播图控制器
 */
@RestController
@RequiredArgsConstructor
public class BannerController {

    private final BannerService bannerService;

    /**
     * 获取有效轮播图列表（公开）
     */
    @GetMapping("/v1/banners")
    public R<List<BannerVO>> activeList() {
        return R.ok(bannerService.activeList());
    }

    /**
     * 管理端-轮播图列表
     */
    @GetMapping("/v1/admin/banners")
    @PreAuthorize("hasRole('ADMIN')")
    public R<List<BannerVO>> adminList() {
        return R.ok(bannerService.adminList());
    }

    /**
     * 管理端-创建轮播图
     */
    @PostMapping("/v1/admin/banners")
    @PreAuthorize("hasRole('ADMIN')")
    public R<Long> create(@Valid @RequestBody BannerRequest request) {
        return R.ok(bannerService.create(
                request.getTitle(),
                request.getImageUrl(),
                request.getLinkUrl(),
                request.getLinkType(),
                request.getSortOrder(),
                request.getStartTime(),
                request.getEndTime(),
                request.getStatus()));
    }

    /**
     * 管理端-更新轮播图
     */
    @PutMapping("/v1/admin/banners/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody BannerRequest request) {
        bannerService.update(
                id,
                request.getTitle(),
                request.getImageUrl(),
                request.getLinkUrl(),
                request.getLinkType(),
                request.getSortOrder(),
                request.getStartTime(),
                request.getEndTime(),
                request.getStatus());
        return R.ok();
    }

    /**
     * 管理端-删除轮播图
     */
    @DeleteMapping("/v1/admin/banners/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public R<Void> delete(@PathVariable Long id) {
        bannerService.delete(id);
        return R.ok();
    }
}
