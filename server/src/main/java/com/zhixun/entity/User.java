package com.zhixun.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.zhixun.enums.RoleEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户实体，对应 sys_user 表
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_user")
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户名 */
    private String username;

    /** 密码哈希，对应 password_hash（默认不查询） */
    @TableField(select = false)
    private String passwordHash;

    /** 昵称 */
    private String nickname;

    /** 头像 */
    private String avatar;

    /** 邮箱 */
    private String email;

    /** 手机号 */
    private String phone;

    /** 角色 */
    private RoleEnum role;

    /** 状态：0-禁用，1-正常 */
    private Integer status;

    /** 个人简介 */
    private String bio;

    /** 是否在线，对应 is_online */
    private Integer isOnline;

    /** 最后活跃时间，对应 last_active_at */
    private LocalDateTime lastActiveAt;

    /** 关注数，对应 follow_count */
    private Integer followCount;

    /** 粉丝数，对应 follower_count */
    private Integer followerCount;

    /** 文章数，对应 article_count */
    private Integer articleCount;

    /** 最后登录时间，对应 last_login_at */
    private LocalDateTime lastLoginAt;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /** 账号状态常量 - 正常 */
    public static final int STATUS_NORMAL = 1;

    /** 账号状态常量 - 禁用 */
    public static final int STATUS_DISABLED = 0;

    /** 角色常量 - 普通用户 */
    public static final String ROLE_USER = "USER";

    /** 角色常量 - 管理员 */
    public static final String ROLE_ADMIN = "ADMIN";
}
