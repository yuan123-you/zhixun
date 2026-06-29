package com.zhixun.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class GroupMessageVO {
    private Long id;
    private Long groupId;
    private Long senderId;
    private String senderName;
    private String senderAvatar;
    private String content;
    private String messageType;
    private List<Long> mentionedUserIds;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}