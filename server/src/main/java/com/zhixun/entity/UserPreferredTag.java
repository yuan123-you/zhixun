package com.zhixun.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.zhixun.enums.PreferredTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户偏好标签关联实体，对应 user_preferred_tag 表
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("user_preferred_tag")
public class UserPreferredTag {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID，对应 user_id */
    private Long userId;

    /** 标签ID，对应 tag_id */
    private Long tagId;

    /** 偏好类型 */
    private PreferredTypeEnum type;
}
