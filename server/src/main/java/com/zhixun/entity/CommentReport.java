package com.zhixun.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 评论举报实体，对应 cms_comment_report 表
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("cms_comment_report")
public class CommentReport {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 被举报的评论ID */
    private Long commentId;

    /** 举报人用户ID */
    private Long reporterId;

    /** 举报原因 */
    private String reason;

    /** 处理状态：0-待处理，1-已处理-忽略，2-已处理-删除评论 */
    private Integer status;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
