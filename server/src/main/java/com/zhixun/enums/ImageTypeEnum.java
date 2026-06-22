package com.zhixun.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 图片类型枚举
 */
@Getter
@AllArgsConstructor
public enum ImageTypeEnum {

    /** 封面图 */
    COVER(1, "封面图"),

    /** 内容图 */
    CONTENT(2, "内容图");

    /** 类型值（存入数据库的值） */
    @EnumValue
    private final Integer value;

    /** 类型描述 */
    private final String description;
}
