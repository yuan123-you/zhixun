package com.zhixun.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 文章状态枚举
 */
@Getter
@AllArgsConstructor
public enum ArticleStatusEnum {

    /** 草稿 */
    DRAFT(0, "草稿"),

    /** 待审核 */
    PENDING(1, "待审核"),

    /** 已发布 */
    PUBLISHED(2, "已发布"),

    /** 已拒绝 */
    REJECTED(3, "已拒绝"),

    /** 已下线 */
    OFFLINE(4, "已下线");

    /** 状态值（存入数据库的值） */
    @EnumValue
    private final Integer value;

    /** 状态描述 */
    private final String description;

    /**
     * 根据 value 获取枚举
     *
     * @param value 状态值
     * @return 枚举实例
     */
    public static ArticleStatusEnum fromValue(Integer value) {
        if (value == null) {
            return null;
        }
        return Arrays.stream(values())
                .filter(e -> e.getValue().equals(value))
                .findFirst()
                .orElse(null);
    }
}
