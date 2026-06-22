package com.zhixun.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 偏好类型枚举
 */
@Getter
@AllArgsConstructor
public enum PreferredTypeEnum {

    /** 感兴趣 */
    INTERESTED(1, "感兴趣"),

    /** 屏蔽 */
    BLOCKED(2, "屏蔽");

    /** 类型值（存入数据库的值） */
    @EnumValue
    private final Integer value;

    /** 类型描述 */
    private final String description;
}
