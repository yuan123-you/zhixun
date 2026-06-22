package com.zhixun.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 关注实体，对应 user_follow 表
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("user_follow")
public class UserFollow {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关注者ID，对应 follower_id */
    private Long followerId;

    /** 被关注者ID，对应 following_id */
    private Long followingId;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
