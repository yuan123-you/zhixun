package com.zhixun.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MessageTypeEnum {
    TEXT("text", "鏂囧瓧"),
    IMAGE("image", "鍥剧墖"),
    VOICE("voice", "璇煶"),
    FILE("file", "鏂囦欢"),
    SYSTEM("system", "绯荤粺");

    private final String code;
    private final String desc;
}