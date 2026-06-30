package com.zhixun.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhixun.common.exception.BusinessException;
import com.zhixun.common.result.ErrorCode;
import com.zhixun.common.result.PageResult;
import com.zhixun.common.util.VOBuilder;
import com.zhixun.config.Master;
import com.zhixun.config.Slave;
import com.zhixun.entity.User;
import com.zhixun.entity.UserFollow;
import com.zhixun.enums.NotificationTypeEnum;
import com.zhixun.mapper.UserFollowMapper;
import com.zhixun.mapper.UserMapper;
import com.zhixun.service.FollowService;
import com.zhixun.service.NotificationService;
import com.zhixun.service.OnlineStatusService;
import com.zhixun.websocket.ChatWebSocketHandler;
import com.zhixun.vo.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.TextMessage;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Collections;
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
    private final OnlineStatusService onlineStatusService;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @Override
    @Master
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

            // 发送关注通知
            sendFollowNotification(userId, targetUser);

            // 检查是否形成互关（对方是否已关注自己），如果是则通知双方可私信
            notifyMutualFollow(userId, targetUserId);
        }

        // 更新 User 表中的 follow_count 和 follower_count（SQL 原子操作）
        if (followed) {
            // 当前用户的关注数 +1
            userMapper.update(null, new LambdaUpdateWrapper<User>()
                    .eq(User::getId, userId)
                    .setSql("follow_count = follow_count + 1"));
            // 目标用户的粉丝数 +1
            userMapper.update(null, new LambdaUpdateWrapper<User>()
                    .eq(User::getId, targetUserId)
                    .setSql("follower_count = follower_count + 1"));
        } else {
            // 当前用户的关注数 -1
            userMapper.update(null, new LambdaUpdateWrapper<User>()
                    .eq(User::getId, userId)
                    .gt(User::getFollowCount, 0)
                    .setSql("follow_count = follow_count - 1"));
            // 目标用户的粉丝数 -1
            userMapper.update(null, new LambdaUpdateWrapper<User>()
                    .eq(User::getId, targetUserId)
                    .gt(User::getFollowerCount, 0)
                    .setSql("follower_count = follower_count - 1"));
        }

        // 获取关注数和粉丝数（从更新后的 User 表读取）
        User updatedUser = userMapper.selectById(userId);
        User updatedTargetUser = userMapper.selectById(targetUserId);
        long followCount = updatedUser != null ? updatedUser.getFollowCount() : 0;
        long followerCount = updatedTargetUser != null ? updatedTargetUser.getFollowerCount() : 0;

        Map<String, Object> result = new HashMap<>();
        result.put("followed", followed);
        result.put("followCount", followCount);
        result.put("followerCount", followerCount);
        return result;
    }

    @Override
    @Slave
    public PageResult<UserVO> getFollowing(Long userId, Integer page, Integer pageSize, String keyword) {
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
            return new PageResult<>(Collections.emptyList(), 0L, page, pageSize);
        }

        // 批量查询用户信息（支持关键词搜索）
        LambdaQueryWrapper<User> userWrapper = new LambdaQueryWrapper<>();
        userWrapper.in(User::getId, followUserIds);
        if (StringUtils.hasText(keyword)) {
            userWrapper.and(w -> w.like(User::getUsername, keyword)
                    .or().like(User::getNickname, keyword));
        }
        List<User> users = userMapper.selectList(userWrapper);

        // 构建带互关和在线状态的 VO 列表
        List<UserVO> voList = buildUserVOListWithStatus(users, userId);

        // 关键词搜索时总数取过滤后的数量
        long total = StringUtils.hasText(keyword) ? users.size() : result.getTotal();
        return new PageResult<>(voList, total, page, pageSize);
    }

    @Override
    @Slave
    public PageResult<UserVO> getFollowers(Long userId, Integer page, Integer pageSize, String keyword) {
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
            return new PageResult<>(Collections.emptyList(), 0L, page, pageSize);
        }

        // 批量查询用户信息（支持关键词搜索）
        LambdaQueryWrapper<User> userWrapper = new LambdaQueryWrapper<>();
        userWrapper.in(User::getId, followerUserIds);
        if (StringUtils.hasText(keyword)) {
            userWrapper.and(w -> w.like(User::getUsername, keyword)
                    .or().like(User::getNickname, keyword));
        }
        List<User> users = userMapper.selectList(userWrapper);

        // 构建带互关和在线状态的 VO 列表
        List<UserVO> voList = buildUserVOListWithStatus(users, userId);

        // 关键词搜索时总数取过滤后的数量
        long total = StringUtils.hasText(keyword) ? users.size() : result.getTotal();
        return new PageResult<>(voList, total, page, pageSize);
    }

    @Override
    @Slave
    public boolean isFollowed(Long userId, Long targetUserId) {
        LambdaQueryWrapper<UserFollow> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserFollow::getFollowerId, userId)
                .eq(UserFollow::getFollowingId, targetUserId);
        return userFollowMapper.selectCount(wrapper) > 0;
    }

    // ========== 内部方法 ==========

    /**
     * 构建用户 VO 列表（含互关状态和在线状态）
     */
    private List<UserVO> buildUserVOListWithStatus(List<User> users, Long currentUserId) {
        if (users.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> userIds = users.stream().map(User::getId).collect(Collectors.toList());

        // 批量查询当前用户是否关注了这些用户（用于判断互关）
        LambdaQueryWrapper<UserFollow> followingWrapper = new LambdaQueryWrapper<>();
        followingWrapper.eq(UserFollow::getFollowerId, currentUserId)
                .in(UserFollow::getFollowingId, userIds);
        List<UserFollow> followingList = userFollowMapper.selectList(followingWrapper);
        // 当前用户关注的用户ID集合
        Map<Long, Boolean> currentFollowingMap = followingList.stream()
                .collect(Collectors.toMap(UserFollow::getFollowingId, f -> true, (a, b) -> a));

        // 批量查询这些用户是否关注了当前用户（用于判断互关）
        LambdaQueryWrapper<UserFollow> followerWrapper = new LambdaQueryWrapper<>();
        followerWrapper.eq(UserFollow::getFollowingId, currentUserId)
                .in(UserFollow::getFollowerId, userIds);
        List<UserFollow> followerList = userFollowMapper.selectList(followerWrapper);
        // 关注了当前用户的用户ID集合
        Map<Long, Boolean> followedByMap = followerList.stream()
                .collect(Collectors.toMap(UserFollow::getFollowerId, f -> true, (a, b) -> a));

        return users.stream().map(user -> {
            UserVO vo = VOBuilder.buildUserVO(user);

            // 判断互关：当前用户关注了对方，且对方也关注了当前用户
            boolean currentFollowsTarget = currentFollowingMap.getOrDefault(user.getId(), false);
            boolean targetFollowsCurrent = followedByMap.getOrDefault(user.getId(), false);
            vo.setIsMutualFollow(currentFollowsTarget && targetFollowsCurrent);

            // 获取在线状态（currentUserId 查看自己不受隐私限制）
            Boolean online = onlineStatusService.getOnlineStatus(user.getId(), currentUserId);
            vo.setIsOnline(online);

            return vo;
        }).collect(Collectors.toList());
    }



    /**
     * 发送关注通知
     */
    private void sendFollowNotification(Long followerId, User targetUser) {
        try {
            User follower = userMapper.selectById(followerId);
            String followerName = follower != null ? follower.getNickname() : "用户";
            String title = "新粉丝";
            String content = followerName + " 关注了你";
            notificationService.createNotification(
                    targetUser.getId(),
                    NotificationTypeEnum.FOLLOW.getValue(),
                    title,
                    content,
                    followerId,
                    "follow:" + followerId
            );
        } catch (Exception e) {
            log.warn("发送关注通知失败: followerId={}, targetUserId={}, error={}", followerId, targetUser.getId(), e.getMessage());
        }
    }

    /**
     * 检查是否形成互关，如果是则通过 WebSocket 通知双方有新的可私信会话
     */
    private void notifyMutualFollow(Long userId, Long targetUserId) {
        try {
            // 检查对方是否已关注自己
            LambdaQueryWrapper<UserFollow> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(UserFollow::getFollowerId, targetUserId)
                    .eq(UserFollow::getFollowingId, userId);
            boolean isMutual = userFollowMapper.selectCount(wrapper) > 0;

            if (!isMutual) return;

            // 获取双方用户信息
            User user = userMapper.selectById(userId);
            User targetUser = userMapper.selectById(targetUserId);
            if (user == null || targetUser == null) return;

            // 通知当前用户：可以和对方私信了
            sendNewConversationNotification(userId, targetUser);
            // 通知对方：可以和当前用户私信了
            sendNewConversationNotification(targetUserId, user);
        } catch (Exception e) {
            log.warn("互关通知失败: userId={}, targetUserId={}, error={}", userId, targetUserId, e.getMessage());
        }
    }

    /**
     * 通过 WebSocket 通知用户有新的可私信会话
     */
    private void sendNewConversationNotification(Long notifyUserId, User otherUser) {
        if (!ChatWebSocketHandler.isUserOnline(notifyUserId)) return;
        try {
            Map<String, Object> wsMessage = Map.of(
                    "type", "NEW_CONVERSATION",
                    "data", Map.of(
                            "userId", otherUser.getId(),
                            "nickname", otherUser.getNickname() != null ? otherUser.getNickname() : "",
                            "avatar", otherUser.getAvatar() != null ? otherUser.getAvatar() : ""
                    )
            );
            String json = objectMapper.writeValueAsString(wsMessage);
            ChatWebSocketHandler.sendToUser(notifyUserId, new TextMessage(json));
        } catch (Exception e) {
            log.warn("发送新会话WebSocket通知失败: notifyUserId={}, error={}", notifyUserId, e.getMessage());
        }
    }
}
