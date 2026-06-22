package com.zhixun.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 角色枚举
 */
@Getter
@AllArgsConstructor
public enum RoleEnum {

    /** 超级管理员 */
    SUPER_ADMIN("SUPER_ADMIN", "超级管理员"),

    /** 管理员 */
    ADMIN("ADMIN", "管理员"),

    /** 普通用户 */
    USER("USER", "普通用户");

    /** 角色标识（存入数据库的值） */
    @EnumValue
    private final String value;

    /** 角色描述 */
    private final String description;
}
