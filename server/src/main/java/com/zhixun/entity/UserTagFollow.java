package com.zhixun.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户关注标签实体，对应 user_tag_follow 表
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("user_tag_follow")
public class UserTagFollow implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID，对应 user_id */
    private Long userId;

    /** 标签ID，对应 tag_id */
    private Long tagId;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
