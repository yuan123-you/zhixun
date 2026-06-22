package com.zhixun.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 评论状态枚举
 */
@Getter
@AllArgsConstructor
public enum CommentStatusEnum {

    /** 待审核 */
    PENDING(0, "待审核"),

    /** 正常 */
    NORMAL(1, "正常"),

    /** 已删除 */
    DELETED(2, "已删除");

    /** 状态值（存入数据库的值） */
    @EnumValue
    private final Integer value;

    /** 状态描述 */
    private final String description;
}
