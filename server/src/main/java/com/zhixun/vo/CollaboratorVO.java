package com.zhixun.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CollaboratorVO {
    private Long id;
    private Long articleId;
    private Long userId;
    private String userName;
    private String userAvatar;
    private String permission;
    private Integer status;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}