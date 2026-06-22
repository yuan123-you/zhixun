package com.zhixun.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 登录用户信息 VO
 */
@Data
public class LoginUserVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 用户ID */
    private Long id;

    /** 用户名 */
    private String username;

    /** 昵称 */
    private String nickname;

    /** 头像URL */
    private String avatar;

    /** 角色 */
    private String role;
}
