package com.zhixun.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 敏感词级别枚举
 */
@Getter
@AllArgsConstructor
public enum SensitiveLevelEnum {

    /** 警告级别 */
    WARN(1, "警告"),

    /** 禁用级别 */
    BANNED(2, "禁用");

    /** 级别值（存入数据库的值） */
    @EnumValue
    private final Integer value;

    /** 级别描述 */
    private final String description;
}
