package com.zhixun.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 入群申请实体，对应 cms_group_join_request
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("cms_group_join_request")
public class GroupJoinRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long groupId;
    private Long userId;
    /** 申请留言 */
    private String message;
    /** 状态：0=待审批，1=已同意，2=已拒绝 */
    private Integer status = STATUS_PENDING;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(exist = false)
    private String userName;
    @TableField(exist = false)
    private String userAvatar;
    @TableField(exist = false)
    private String groupName;

    public static final int STATUS_PENDING = 0;
    public static final int STATUS_APPROVED = 1;
    public static final int STATUS_REJECTED = 2;
}
