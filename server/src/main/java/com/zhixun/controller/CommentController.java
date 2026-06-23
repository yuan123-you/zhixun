package com.zhixun.controller;

import com.zhixun.common.annotation.OperationLog;
import com.zhixun.common.result.PageResult;
import com.zhixun.common.result.R;
import com.zhixun.common.util.SecurityUtil;
import com.zhixun.dto.comment.CommentAuditRequest;
import com.zhixun.dto.comment.CommentCreateRequest;
import com.zhixun.dto.comment.CommentReportRequest;
import com.zhixun.service.CommentService;
import com.zhixun.vo.CommentReportVO;
import com.zhixun.vo.CommentVO;
import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
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
 * 评论控制器
 */
@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;
    private final SecurityUtil securityUtil;

    /**
     * 发表评论
     */
    @PostMapping("/articles/{id}/comments")
    @PreAuthorize("isAuthenticated()")
    @OperationLog(module = "评论", action = "发表")
    @SentinelResource(value = "comment-create", blockHandler = "createCommentBlockHandler", blockHandlerClass = CommentController.BlockHandlers.class)
    public R<Long> createComment(@PathVariable Long id,
                                  @Valid @RequestBody CommentCreateRequest request) {
        Long userId = securityUtil.getCurrentUserId();
        // 确保 articleId 与路径一致
        request.setArticleId(id);
        return R.ok(commentService.createComment(userId, request));
    }

    /**
     * 评论列表
     */
    @GetMapping("/articles/{id}/comments")
    public R<PageResult<CommentVO>> getComments(
            @PathVariable Long id,
            @RequestParam(defaultValue = "latest") String sort,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return R.ok(commentService.getComments(id, sort, page, pageSize));
    }

    /**
     * 删除评论
     */
    @DeleteMapping("/comments/{id}")
    @PreAuthorize("isAuthenticated()")
    @OperationLog(module = "评论", action = "删除")
    public R<Void> deleteComment(@PathVariable Long id) {
        Long userId = securityUtil.getCurrentUserId();
        commentService.deleteComment(userId, id);
        return R.ok();
    }

    /**
     * 审核评论（管理员）
     */
    @PutMapping("/comments/{id}/audit")
    @PreAuthorize("hasRole('ADMIN')")
    public R<Void> auditComment(@PathVariable Long id, @Valid @RequestBody CommentAuditRequest request) {
        Long userId = securityUtil.getCurrentUserId();
        commentService.auditComment(userId, id, request.getAction(), request.getReason());
        return R.ok();
    }

    /**
     * 举报评论
     */
    @PostMapping("/comments/{id}/report")
    @PreAuthorize("isAuthenticated()")
    @OperationLog(module = "评论", action = "举报")
    public R<Void> reportComment(@PathVariable Long id,
                                  @Valid @RequestBody CommentReportRequest request) {
        Long userId = securityUtil.getCurrentUserId();
        commentService.reportComment(userId, id, request);
        return R.ok();
    }

    /**
     * 获取评论举报列表（管理员）
     */
    @GetMapping("/admin/comments/reports")
    @PreAuthorize("hasRole('ADMIN')")
    public R<PageResult<CommentReportVO>> getReports(
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return R.ok(commentService.getReports(status, page, pageSize));
    }

    /**
     * Sentinel 限流降级处理
     */
    public static class BlockHandlers {
        public static R<Long> createCommentBlockHandler(Long id, CommentCreateRequest request, BlockException e) {
            return R.fail(429, "评论请求过于频繁，请稍后重试");
        }
    }
}
