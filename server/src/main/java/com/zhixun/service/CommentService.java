package com.zhixun.service;

import com.zhixun.common.result.PageResult;
import com.zhixun.dto.comment.CommentCreateRequest;
import com.zhixun.vo.CommentVO;

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
}
