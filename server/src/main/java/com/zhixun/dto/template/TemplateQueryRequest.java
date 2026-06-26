package com.zhixun.dto.template;

import lombok.Data;

@Data
public class TemplateQueryRequest {
    private Integer page = 1;
    private Integer pageSize = 20;
    private String category;
    private String keyword;
}