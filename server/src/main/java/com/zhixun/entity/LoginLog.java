package com.zhixun.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 登录日志实体，对应 sys_login_log 表
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_login_log")
public class LoginLog implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 登录用户ID，对应 user_id */
    private Long userId;

    /** 登录用户名 */
    private String username;

    /** 用户昵称 */
    @TableField(exist = false)
    private String nickname;

    /** 登录状态：0-失败，1-成功 */
    private Integer status;

    /** 登录IP */
    private String ip;

    /** 登录地点 */
    private String location;

    /** 用户代理，对应 user_agent */
    private String userAgent;

    /** 失败原因，对应 fail_reason */
    private String failReason;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
