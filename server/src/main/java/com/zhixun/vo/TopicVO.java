package com.zhixun.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TopicVO {
    private Long id;
    private String name;
    private String description;
    private String coverImage;
    private Long articleCount;
    private Long followCount;
    private BigDecimal hotScore;
    private Integer isOfficial;
    private Integer status;
    private Boolean isFollowed;
}