package com.zhixun.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class GroupMemberVO {
    private Long id;
    private Long groupId;
    private Long userId;
    private String userName;
    private String userAvatar;
    private String nickname;
    private Integer role;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime joinedAt;
}
