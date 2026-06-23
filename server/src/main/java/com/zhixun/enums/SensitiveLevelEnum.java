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

    /** 低级：替换为*** */
    LOW(1, "低级"),

    /** 中级：替换为***，并记录日志 */
    MEDIUM(2, "中级"),

    /** 高级：直接拦截内容 */
    HIGH(3, "高级");

    /** 级别值（存入数据库的值） */
    @EnumValue
    private final Integer value;

    /** 级别描述 */
    private final String description;
}
