package com.zhixun.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BadgeVO {
    private Long id;
    private String name;
    private String description;
    private String icon;
    private String category;
    private Boolean isOwned;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime earnedAt;
}