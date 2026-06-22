package com.zhixun.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 通知类型枚举
 */
@Getter
@AllArgsConstructor
public enum NotificationTypeEnum {

    /** 系统通知 */
    SYSTEM(1, "系统通知"),

    /** 审核通知 */
    AUDIT(2, "审核通知"),

    /** 互动通知 */
    INTERACT(3, "互动通知"),

    /** 关注通知 */
    FOLLOW(4, "关注通知"),

    /** 私信通知 */
    MESSAGE(5, "私信通知");

    /** 类型值（存入数据库的值） */
    @EnumValue
    private final Integer value;

    /** 类型描述 */
    private final String description;
}
