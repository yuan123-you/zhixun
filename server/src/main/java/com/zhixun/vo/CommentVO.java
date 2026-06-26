package com.zhixun.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 评论视图（含用户信息、子评论）
 */
@Data
public class CommentVO {

    /** 评论ID */
    private Long id;

    /** 作品ID */
    private Long articleId;

    /** 评论内容 */
    private String content;

    /** 评论状态 */
    private Integer status;

    /** 点赞数 */
    private Integer likeCount;

    /** 评论用户ID */
    private Long userId;

    /** 评论用户信息 */
    private UserVO user;

    /** 父评论ID */
    private Long parentId;

    /** 回复目标用户信息 */
    private UserVO replyUser;

    /** 子评论列表 */
    private List<CommentVO> replies;

    /** 当前用户是否已点赞 */
    private Boolean isLiked;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
