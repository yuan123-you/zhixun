package com.zhixun.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户信息视图（脱敏，不含密码）
 */
@Data
public class UserVO {

    /** 用户ID */
    private Long id;

    /** 用户名 */
    private String username;

    /** 昵称 */
    private String nickname;

    /** 头像 */
    private String avatar;

    /** 邮箱 */
    private String email;

    /** 手机号 */
    private String phone;

    /** 角色 */
    private String role;

    /** 状态 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 是否互相关注 */
    private Boolean isMutualFollow;

    /** 是否在线 */
    private Boolean isOnline;
}
