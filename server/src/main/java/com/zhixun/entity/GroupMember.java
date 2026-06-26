package com.zhixun.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("cms_group_member")
public class GroupMember implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long groupId;
    private Long userId;
    private Integer role = 0;
    private String nickname;
    private Integer unreadCount = 0;
    private Integer isMuted = 0;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime joinedAt;

    @TableField(exist = false)
    private String userName;

    @TableField(exist = false)
    private String userAvatar;

    public static final int ROLE_MEMBER = 0;
    public static final int ROLE_ADMIN = 1;
    public static final int ROLE_OWNER = 2;
}
