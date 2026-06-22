package com.zhixun.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 收藏实体，对应 cms_collect 表
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("cms_collect")
public class Collect {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID，对应 user_id */
    private Long userId;

    /** 文章ID，对应 article_id */
    private Long articleId;

    /** 分组名称，对应 group_name */
    private String groupName;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
