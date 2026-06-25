package com.zhixun.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 文章可见性枚举
 */
@Getter
@AllArgsConstructor
public enum ArticleVisibilityEnum {

    /** 公开 */
    PUBLIC(0, "公开"),

    /** 仅粉丝可见 */
    FOLLOWERS(1, "仅粉丝可见"),

    /** 仅互相关注的人可见 */
    MUTUAL(2, "仅互关可见"),

    /** 仅自己可见 */
    PRIVATE(3, "仅自己可见");

    /** 可见性值 */
    private final Integer value;

    /** 可见性描述 */
    private final String description;

    /**
     * 根据 value 获取枚举
     */
    public static ArticleVisibilityEnum fromValue(Integer value) {
        if (value == null) {
            return PUBLIC;
        }
        return Arrays.stream(values())
                .filter(e -> e.getValue().equals(value))
                .findFirst()
                .orElse(PUBLIC);
    }
}
