package com.zhixun.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.zhixun.enums.LikeTargetTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 点赞实体，对应 cms_like 表
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("cms_like")
public class ArticleLike {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID，对应 user_id */
    private Long userId;

    /** 目标ID，对应 target_id */
    private Long targetId;

    /** 目标类型，对应 target_type */
    private LikeTargetTypeEnum targetType;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
