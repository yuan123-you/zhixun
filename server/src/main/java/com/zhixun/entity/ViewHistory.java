package com.zhixun.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 浏览记录实体，对应 cms_view_history 表
 * 注意：ip 和 user_agent 字段已移除，改为客户端本地存储
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

    /** 作品ID，对应 article_id */
    private Long articleId;

    /** 浏览时长，对应 view_duration */
    private Integer viewDuration;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
