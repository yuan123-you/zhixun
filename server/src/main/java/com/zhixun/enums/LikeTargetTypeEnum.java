package com.zhixun.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 点赞目标类型枚举
 */
@Getter
@AllArgsConstructor
public enum LikeTargetTypeEnum {

    /** 文章 */
    ARTICLE(1, "文章"),

    /** 评论 */
    COMMENT(2, "评论");

    /** 类型值（存入数据库的值） */
    @EnumValue
    private final Integer value;

    /** 类型描述 */
    private final String description;
}
