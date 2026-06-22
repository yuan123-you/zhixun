package com.zhixun.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 浏览记录实体，对应 cms_view_history 表
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("cms_view_history")
public class ViewHistory {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID，对应 user_id */
    private Long userId;

    /** 文章ID，对应 article_id */
    private Long articleId;

    /** 访问IP */
    private String ip;

    /** 用户代理，对应 user_agent */
    private String userAgent;

    /** 浏览时长，对应 view_duration */
    private Integer viewDuration;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
