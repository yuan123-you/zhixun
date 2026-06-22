package com.zhixun.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.zhixun.enums.PreferredTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户偏好分类关联实体，对应 user_preferred_category 表
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("user_preferred_category")
public class UserPreferredCategory {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID，对应 user_id */
    private Long userId;

    /** 分类ID，对应 category_id */
    private Long categoryId;

    /** 偏好类型 */
    private PreferredTypeEnum type;
}
