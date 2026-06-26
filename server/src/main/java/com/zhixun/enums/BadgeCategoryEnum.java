package com.zhixun.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum BadgeCategoryEnum {
    SIGN_IN("sign_in", "绛惧埌"),
    CONTENT("content", "鍒涗綔"),
    SOCIAL("social", "绀句氦"),
    ACHIEVEMENT("achievement", "鎴愬氨"),
    SPECIAL("special", "鐗规畩");

    private final String code;
    private final String desc;
}