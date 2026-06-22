package com.zhixun.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhixun.common.exception.BusinessException;
import com.zhixun.common.result.ErrorCode;
import com.zhixun.common.result.PageResult;
import com.zhixun.entity.User;
import com.zhixun.entity.UserFollow;
import com.zhixun.mapper.UserFollowMapper;
import com.zhixun.mapper.UserMapper;
import com.zhixun.service.FollowService;
import com.zhixun.vo.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 关注服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FollowServiceImpl implements FollowService {

    private final UserFollowMapper userFollowMapper;
    private final UserMapper userMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> toggleFollow(Long userId, Long targetUserId) {
        // 检查不能关注自己
        if (userId.equals(targetUserId)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不能关注自己");
        }

        // 检查目标用户是否存在
        User targetUser = userMapper.selectById(targetUserId);
        if (targetUser == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        // 查询是否已关注
        LambdaQueryWrapper<UserFollow> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserFollow::getFollowerId, userId)
                .eq(UserFollow::getFollowingId, targetUserId);
        UserFollow existingFollow = userFollowMapper.selectOne(wrapper);

        boolean followed;
        if (existingFollow != null) {
            // 已关注，取消关注
            userFollowMapper.deleteById(existingFollow.getId());
            followed = false;
        } else {
            // 未关注，创建关注记录
            UserFollow follow = new UserFollow();
            follow.setFollowerId(userId);
            follow.setFollowingId(targetUserId);
            userFollowMapper.insert(follow);
            followed = true;
        }

        // 获取关注数和粉丝数
        long followCount = userFollowMapper.selectCount(
                new LambdaQueryWrapper<UserFollow>().eq(UserFollow::getFollowerId, userId));
        long followerCount = userFollowMapper.selectCount(
                new LambdaQueryWrapper<UserFollow>().eq(UserFollow::getFollowingId, targetUserId));

        Map<String, Object> result = new HashMap<>();
        result.put("followed", followed);
        result.put("follow_count", followCount);
        result.put("follower_count", followerCount);
        return result;
    }

    @Override
    public PageResult<UserVO> getFollowing(Long userId, Integer page, Integer pageSize) {
        // 查询关注列表
        LambdaQueryWrapper<UserFollow> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserFollow::getFollowerId, userId)
                .orderByDesc(UserFollow::getCreatedAt);
        Page<UserFollow> followPage = new Page<>(page, pageSize);
        Page<UserFollow> result = userFollowMapper.selectPage(followPage, wrapper);

        // 获取关注的用户ID列表
        List<Long> followUserIds = result.getRecords().stream()
                .map(UserFollow::getFollowingId)
                .collect(Collectors.toList());

        if (followUserIds.isEmpty()) {
            return new PageResult<>(java.util.Collections.emptyList(), 0L, page, pageSize);
        }

        // 批量查询用户信息
        List<User> users = userMapper.selectBatchIds(followUserIds);
        List<UserVO> voList = users.stream()
                .map(this::buildUserVO)
                .collect(Collectors.toList());

        return new PageResult<>(voList, result.getTotal(), page, pageSize);
    }

    @Override
    public PageResult<UserVO> getFollowers(Long userId, Integer page, Integer pageSize) {
        // 查询粉丝列表
        LambdaQueryWrapper<UserFollow> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserFollow::getFollowingId, userId)
                .orderByDesc(UserFollow::getCreatedAt);
        Page<UserFollow> followPage = new Page<>(page, pageSize);
        Page<UserFollow> result = userFollowMapper.selectPage(followPage, wrapper);

        // 获取粉丝用户ID列表
        List<Long> followerUserIds = result.getRecords().stream()
                .map(UserFollow::getFollowerId)
                .collect(Collectors.toList());

        if (followerUserIds.isEmpty()) {
            return new PageResult<>(java.util.Collections.emptyList(), 0L, page, pageSize);
        }

        // 批量查询用户信息
        List<User> users = userMapper.selectBatchIds(followerUserIds);
        List<UserVO> voList = users.stream()
                .map(this::buildUserVO)
                .collect(Collectors.toList());

        return new PageResult<>(voList, result.getTotal(), page, pageSize);
    }

    @Override
    public boolean isFollowed(Long userId, Long targetUserId) {
        LambdaQueryWrapper<UserFollow> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserFollow::getFollowerId, userId)
                .eq(UserFollow::getFollowingId, targetUserId);
        return userFollowMapper.selectCount(wrapper) > 0;
    }

    // ========== 内部方法 ==========

    /**
     * 构建用户 VO（脱敏）
     */
    private UserVO buildUserVO(User user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setRole(user.getRole() != null ? user.getRole().name() : null);
        vo.setStatus(user.getStatus());
        vo.setCreatedAt(user.getCreatedAt());
        return vo;
    }
}
