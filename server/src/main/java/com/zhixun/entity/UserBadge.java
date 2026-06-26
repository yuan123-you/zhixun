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
@TableName("user_badge")
public class UserBadge implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long badgeId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime earnedAt;

    @TableField(exist = false)
    private String badgeName;
    @TableField(exist = false)
    private String badgeIcon;
    @TableField(exist = false)
    private String badgeDescription;
    @TableField(exist = false)
    private String badgeCategory;
}
