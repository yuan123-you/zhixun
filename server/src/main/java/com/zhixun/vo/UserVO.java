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

    /** 用户唯一标识号 */
    private String uid;

    /** 用户名 */
    private String username;

    /** 昵称 */
    private String nickname;

    /** 头像 */
    private String avatar;

    /** 个人简介 */
    private String bio;

    /** 所属省份 */
    private String province;

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

    /** 关注数 */
    private Integer followCount;

    /** 粉丝数 */
    private Integer followerCount;

    /** 文章数 */
    private Integer articleCount;

    /** 是否已关注（当前用户是否关注了此用户） */
    private Boolean isFollowing;
}
