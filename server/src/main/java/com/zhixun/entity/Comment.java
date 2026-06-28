package com.zhixun.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.zhixun.enums.CommentStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 评论实体，对应 cms_comment 表
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("cms_comment")
public class Comment {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 作品ID，对应 article_id */
    private Long articleId;

    /** 评论用户ID，对应 user_id */
    private Long userId;

    /** 父评论ID，对应 parent_id */
    private Long parentId;

    /** 回复目标用户ID，对应 reply_to_id */
    private Long replyToId;

    /** 评论内容 */
    private String content;

    /** 评论图片列表（JSON 字符串存储） */
    private String images;

    /** 评论状态 */
    private CommentStatusEnum status;

    /** 点赞数，对应 like_count */
    private Integer likeCount;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 删除时间，DATETIME类型，NULL=未删除，非NULL=已删除 */
    private LocalDateTime deletedAt;
}
