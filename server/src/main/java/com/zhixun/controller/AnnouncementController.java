package com.zhixun.controller;

import com.zhixun.common.result.R;
import com.zhixun.dto.announcement.AnnouncementRequest;
import com.zhixun.service.AnnouncementService;
import com.zhixun.vo.AnnouncementVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 公告控制器
 */
@RestController
@RequiredArgsConstructor
public class AnnouncementController {

    private final AnnouncementService announcementService;

    /**
     * 获取有效公告列表（公开）
     */
    @GetMapping("/v1/announcements")
    public R<List<AnnouncementVO>> activeList() {
        return R.ok(announcementService.activeList());
    }

    /**
     * 管理端-公告列表
     */
    @GetMapping("/v1/admin/announcements")
    @PreAuthorize("hasRole('ADMIN')")
    public R<List<AnnouncementVO>> adminList() {
        return R.ok(announcementService.adminList());
    }

    /**
     * 管理端-创建公告
     */
    @PostMapping("/v1/admin/announcements")
    @PreAuthorize("hasRole('ADMIN')")
    public R<Long> create(@Valid @RequestBody AnnouncementRequest request) {
        return R.ok(announcementService.create(
                request.getTitle(),
                request.getContent(),
                request.getType(),
                request.getIsTop(),
                request.getStartTime(),
                request.getEndTime(),
                request.getStatus()));
    }

    /**
     * 管理端-更新公告
     */
    @PutMapping("/v1/admin/announcements/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody AnnouncementRequest request) {
        announcementService.update(
                id,
                request.getTitle(),
                request.getContent(),
                request.getType(),
                request.getIsTop(),
                request.getStartTime(),
                request.getEndTime(),
                request.getStatus());
        return R.ok();
    }

    /**
     * 管理端-删除公告
     */
    @DeleteMapping("/v1/admin/announcements/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public R<Void> delete(@PathVariable Long id) {
        announcementService.delete(id);
        return R.ok();
    }
}
