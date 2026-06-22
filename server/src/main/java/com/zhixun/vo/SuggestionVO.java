package com.zhixun.vo;

import lombok.Data;

@Data
public class SuggestionVO {
    private String type;  // user, article, tag
    private Long id;
    private String text;
    private String avatar;  // only for user type
}
