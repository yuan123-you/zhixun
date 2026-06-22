package com.zhixun.service;

import com.zhixun.common.result.PageResult;
import com.zhixun.vo.UserVO;

import java.util.Map;

/**
 * 关注服务接口
 */
public interface FollowService {

    /**
     * 关注/取消关注（切换逻辑）
     *
     * @param userId       用户ID
     * @param targetUserId 目标用户ID
     * @return 包含 followed、follow_count、follower_count 的结果
     */
    Map<String, Object> toggleFollow(Long userId, Long targetUserId);

    /**
     * 获取关注列表
     *
     * @param userId   用户ID
     * @param page     页码
     * @param pageSize 每页大小
     * @return 用户分页列表
     */
    PageResult<UserVO> getFollowing(Long userId, Integer page, Integer pageSize);

    /**
     * 获取粉丝列表
     *
     * @param userId   用户ID
     * @param page     页码
     * @param pageSize 每页大小
     * @return 用户分页列表
     */
    PageResult<UserVO> getFollowers(Long userId, Integer page, Integer pageSize);

    /**
     * 检查关注关系
     *
     * @param userId       用户ID
     * @param targetUserId 目标用户ID
     * @return true-已关注
     */
    boolean isFollowed(Long userId, Long targetUserId);
}
