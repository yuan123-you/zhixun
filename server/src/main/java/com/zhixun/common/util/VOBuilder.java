package com.zhixun.common.util;

import com.zhixun.entity.User;
import com.zhixun.vo.UserVO;

/**
 * VO 构建工具类
 * 统一管理实体到 VO 的转换，消除各处重复的 buildVO 方法
 */
public final class VOBuilder {

    private VOBuilder() {}

    /**
     * 构建用户 VO（基础脱敏版）
     * 用于评论、关注等场景的用户信息展示
     */
    public static UserVO buildUserVO(User user) {
        if (user == null) return null;
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setRole(user.getRole() != null ? user.getRole().name() : null);
        vo.setStatus(user.getStatus());
        vo.setCreatedAt(user.getCreatedAt());
        vo.setFollowCount(user.getFollowCount());
        vo.setFollowerCount(user.getFollowerCount());
        vo.setArticleCount(user.getArticleCount());
        vo.setBio(user.getBio());
        vo.setProvince(user.getProvince());
        vo.setIpLocation(user.getIpLocation());
        return vo;
    }
}
