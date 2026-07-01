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

    /** 性别：0=未知，1=男，2=女 */
    private Integer gender;

    /** 所属省份 */
    private String province;

    /** IP属地 */
    private String ipLocation;

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

    /** 作品数 */
    private Integer articleCount;

    /** 是否已关注（当前用户是否关注了此用户） */
    private Boolean isFollowing;

    /** 获赞总数（所有作品的总点赞数，含隐藏作品） */
    private Long totalLikeCount;

    /** 是否在个人主页展示性别 */
    private Boolean showGenderOnProfile;

    /** 个人简介 */
    private String bio;

    /** 最后登录时间 */
    private java.time.LocalDateTime lastLoginAt;

    /** 最后登录IP */
    private String lastLoginIp;

    /** 登录次数 */
    private Integer loginCount;

    /** 最后活跃时间 */
    private java.time.LocalDateTime lastActiveAt;
}
