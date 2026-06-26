package com.zhixun.dto.topic;

import lombok.Data;

@Data
public class TopicQueryRequest {
    private Integer page = 1;
    private Integer pageSize = 20;
    private String keyword;
    private String orderBy = "hot";
}