package com.zhixun.service;

import com.zhixun.common.result.PageResult;
import com.zhixun.dto.comment.CommentCreateRequest;
import com.zhixun.dto.comment.CommentReportRequest;
import com.zhixun.vo.CommentVO;
import com.zhixun.vo.CommentReportVO;

/**
 * 评论服务接口
 */
public interface CommentService {

    /**
     * 发表评论
     *
     * @param userId  用户ID
     * @param request 评论创建请求
     * @return 评论ID
     */
    Long createComment(Long userId, CommentCreateRequest request);

    /**
     * 获取评论列表（树形结构）
     *
     * @param articleId 文章ID
     * @param sort      排序方式（latest/hot）
     * @param page      页码
     * @param pageSize  每页大小
     * @return 评论分页列表
     */
    PageResult<CommentVO> getComments(Long articleId, String sort, Integer page, Integer pageSize);

    /**
     * 删除评论（软删除）
     *
     * @param userId    用户ID
     * @param commentId 评论ID
     */
    void deleteComment(Long userId, Long commentId);

    /**
     * 获取我的评论列表
     *
     * @param userId   用户ID
     * @param page     页码
     * @param pageSize 每页大小
     * @return 评论分页列表
     */
    PageResult<CommentVO> getMyComments(Long userId, Integer page, Integer pageSize);

    /**
     * 审核评论
     *
     * @param userId    管理员ID
     * @param commentId 评论ID
     * @param action    审核动作（approve/reject）
     * @param reason    审核意见/拒绝原因
     */
    void auditComment(Long userId, Long commentId, String action, String reason);

    /**
     * 获取待审核评论列表
     *
     * @param page     页码
     * @param pageSize 每页大小
     * @return 待审核评论分页列表
     */
    PageResult<CommentVO> getPendingComments(Integer page, Integer pageSize);

    /**
     * 举报评论
     *
     * @param userId    举报人ID
     * @param commentId 评论ID
     * @param request   举报请求
     */
    void reportComment(Long userId, Long commentId, CommentReportRequest request);

    /**
     * 获取评论举报列表（管理员）
     *
     * @param status   处理状态（可选）
     * @param page     页码
     * @param pageSize 每页大小
     * @return 举报分页列表
     */
    PageResult<CommentReportVO> getReports(Integer status, Integer page, Integer pageSize);
}
