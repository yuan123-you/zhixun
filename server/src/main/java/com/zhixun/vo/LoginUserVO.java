package com.zhixun.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 登录用户信息 VO
 */
@Data
public class LoginUserVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 用户ID */
    private Long id;

    /** 用户唯一标识号 */
    private String uid;

    /** 用户名 */
    private String username;

    /** 昵称 */
    private String nickname;

    /** 头像URL */
    private String avatar;

    /** 角色 */
    private String role;

    /** 权限列表 */
    private List<String> permissions;
}
