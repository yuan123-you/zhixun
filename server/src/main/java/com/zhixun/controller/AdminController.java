package com.zhixun.controller;

import com.zhixun.common.result.PageResult;
import com.zhixun.common.result.R;
import com.zhixun.common.util.SecurityUtil;
import com.zhixun.dto.admin.AuditRequest;
import com.zhixun.dto.admin.SensitiveWordRequest;
import com.zhixun.dto.admin.UserStatusRequest;
import com.zhixun.entity.OperationLog;
import com.zhixun.entity.SensitiveWord;
import com.zhixun.service.AdminService;
import com.zhixun.service.OperationLogService;
import com.zhixun.service.OpenSearchSyncService;
import com.zhixun.vo.ArticleVO;
import com.zhixun.vo.CommentVO;
import com.zhixun.vo.DashboardVO;
import com.zhixun.vo.UserVO;
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

/**
 * 后台管理控制器
 */
@RestController
@RequestMapping("/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;
    private final OperationLogService operationLogService;
    private final SecurityUtil securityUtil;
    private final OpenSearchSyncService openSearchSyncService;

    /**
     * 待审核文章列表
     */
    @GetMapping("/articles/pending")
    public R<PageResult<ArticleVO>> pendingArticles(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return R.ok(adminService.getPendingArticles(page, pageSize));
    }

    /**
     * 审核文章
     */
    @PutMapping("/articles/{id}/audit")
    public R<Void> auditArticle(@PathVariable Long id, @Valid @RequestBody AuditRequest request) {
        Long adminId = securityUtil.getCurrentUserId();
        adminService.auditArticle(adminId, id, request);
        return R.ok();
    }

    /**
     * 数据概览
     */
    @GetMapping("/dashboard/overview")
    public R<DashboardVO> dashboardOverview() {
        return R.ok(adminService.getDashboardOverview());
    }

    /**
     * 用户列表
     */
    @GetMapping("/users")
    public R<PageResult<UserVO>> userList(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return R.ok(adminService.getUserList(keyword, role, status, page, pageSize));
    }

    /**
     * 封禁/解封用户
     */
    @PutMapping("/users/{id}/status")
    public R<Void> updateUserStatus(@PathVariable Long id, @Valid @RequestBody UserStatusRequest request) {
        Long adminId = securityUtil.getCurrentUserId();
        adminService.updateUserStatus(adminId, id, request);
        return R.ok();
    }

    /**
     * 敏感词列表
     */
    @GetMapping("/sensitive-words")
    public R<PageResult<SensitiveWord>> sensitiveWords(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return R.ok(adminService.getSensitiveWords(page, pageSize));
    }

    /**
     * 添加敏感词
     */
    @PostMapping("/sensitive-words")
    public R<Void> addSensitiveWord(@Valid @RequestBody SensitiveWordRequest request) {
        Long adminId = securityUtil.getCurrentUserId();
        adminService.addSensitiveWord(adminId, request);
        return R.ok();
    }

    /**
     * 删除敏感词
     */
    @DeleteMapping("/sensitive-words/{id}")
    public R<Void> deleteSensitiveWord(@PathVariable Long id) {
        Long adminId = securityUtil.getCurrentUserId();
        adminService.deleteSensitiveWord(adminId, id);
        return R.ok();
    }

    /**
     * 操作日志
     */
    @GetMapping("/operation-logs")
    public R<PageResult<OperationLog>> operationLogs(
            @RequestParam(required = false) Long operatorId,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return R.ok(operationLogService.getLogs(operatorId, module, startDate, endDate, page, pageSize));
    }

    /**
     * 全量同步数据到 OpenSearch
     */
    @PostMapping("/search/sync")
    public R<Void> syncOpenSearch() {
        openSearchSyncService.fullSync();
        return R.ok();
    }

    /**
     * 评论列表（管理员）
     */
    @GetMapping("/comments")
    public R<PageResult<CommentVO>> commentList(
            @RequestParam(required = false) Long articleId,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return R.ok(adminService.getCommentList(articleId, status, page, pageSize));
    }

    /**
     * 删除评论（管理员）
     */
    @DeleteMapping("/comments/{id}")
    public R<Void> deleteComment(@PathVariable Long id) {
        Long adminId = securityUtil.getCurrentUserId();
        adminService.deleteCommentAsAdmin(adminId, id);
        return R.ok();
    }
}
