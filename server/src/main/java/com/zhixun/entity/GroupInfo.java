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
@TableName("cms_group_info")
public class GroupInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;
    private String avatar;
    private String description;
    private Long ownerId;
    private Long memberCount = 1L;
    private Integer maxMembers = 200;
    private Integer isPublic = 1;
    private Integer status = 0;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableField(exist = false)
    private String ownerName;

    @TableField(exist = false)
    private String ownerAvatar;

    @TableField(exist = false)
    private Integer myRole;

    public static final int STATUS_NORMAL = 0;
    public static final int STATUS_DISMISSED = 1;
}
