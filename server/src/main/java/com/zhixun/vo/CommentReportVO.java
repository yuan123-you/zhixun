package com.zhixun.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 评论举报视图对象
 */
@Data
public class CommentReportVO {

    /** 举报ID */
    private Long id;

    /** 被举报的评论ID */
    private Long commentId;

    /** 评论内容摘要 */
    private String commentContent;

    /** 举报人ID */
    private Long reporterId;

    /** 举报人昵称 */
    private String reporterNickname;

    /** 举报原因 */
    private String reason;

    /** 处理状态：0-待处理，1-已处理-忽略，2-已处理-删除评论 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
