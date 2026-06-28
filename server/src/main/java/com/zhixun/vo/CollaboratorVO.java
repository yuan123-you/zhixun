package com.zhixun.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CollaboratorVO {
    private Long id;
    private Long articleId;
    /** 文章标题（管理端用） */
    private String articleTitle;
    private Long userId;
    private String userName;
    private String userAvatar;
    /** 邀请者ID（管理端用） */
    private Long inviterId;
    /** 邀请者名称（管理端用） */
    private String inviterName;
    /** 被邀请者ID（管理端用） */
    private Long inviteeId;
    /** 被邀请者名称（管理端用） */
    private String inviteeName;
    private String permission;
    private Integer status;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}