package com.zhixun.vo;

import lombok.Data;

@Data
public class TemplateVO {
    private Long id;
    private String name;
    private String description;
    private String coverImage;
    private String category;
    private String content;
    private String tags;
    private Long useCount;
    private String creatorName;
}