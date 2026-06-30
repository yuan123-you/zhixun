package com.zhixun.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhixun.dto.group.GroupCreateRequest;
import com.zhixun.dto.group.GroupInviteRequest;
import com.zhixun.dto.group.GroupMessageRequest;
import com.zhixun.common.exception.BusinessException;
import com.zhixun.common.result.ErrorCode;
import com.zhixun.entity.*;
import com.zhixun.mapper.*;
import com.zhixun.service.AIService;
import com.zhixun.dto.ai.AIWriteRequest;
import com.zhixun.vo.AIResponseVO;
import com.zhixun.security.HtmlWhitelistFilter;
import com.zhixun.common.util.SensitiveWordUtil;
import com.zhixun.service.GroupService;
import com.zhixun.websocket.GroupChatWebSocketHandler;
import com.zhixun.vo.GroupJoinRequestVO;
import com.zhixun.vo.GroupMemberVO;
import com.zhixun.vo.GroupMessageVO;
import com.zhixun.vo.GroupVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GroupServiceImpl implements GroupService {
    private final GroupMapper groupMapper;
    private final GroupMemberMapper groupMemberMapper;
    private final GroupMessageMapper groupMessageMapper;
    private final GroupJoinRequestMapper groupJoinRequestMapper;
    private final UserMapper userMapper;
    private final AIService aiService;
    private final SensitiveWordUtil sensitiveWordUtil;
    private final ObjectMapper objectMapper;

    @Override @Transactional
    public Long createGroup(Long userId, GroupCreateRequest request) {
        // 最多重试5次以容忍 group_number 唯一索引的极小概率碰撞
        for (int i = 0; i < 5; i++) {
            try {
                GroupInfo g = new GroupInfo();
                g.setName(request.getName());
                g.setDescription(request.getDescription());
                g.setAvatar(request.getAvatar());
                g.setOwnerId(userId);
                g.setIsPublic(request.getIsPublic() != null ? request.getIsPublic() : 1);
                g.setGroupNumber(generateUniqueGroupNumber());
                groupMapper.insert(g);
                GroupMember gm = new GroupMember();
                gm.setGroupId(g.getId()); gm.setUserId(userId);
                gm.setRole(GroupMember.ROLE_OWNER);
                gm.setJoinedAt(java.time.LocalDateTime.now());
                groupMemberMapper.insert(gm);
                return g.getId();
            } catch (org.springframework.dao.DuplicateKeyException e) {
                // group_number 唯一冲突，重试
                if (i == 4) throw e;
            }
        }
        log.error("创建群组失败，group_number 唯一索引碰撞超过最大重试次数");
        throw new BusinessException(ErrorCode.BUSINESS_ERROR, "创建群组失败，请重试");
    }

    /**
     * 生成6-10位不重复的纯数字群号，长度严格<=10，碰撞自动重试10次。
     * 使用 ThreadLocalRandom 避免并发争用；首位不能为 0，所以固定从 10^(digits-1) 起取整。
     */
    private String generateUniqueGroupNumber() {
        java.util.concurrent.ThreadLocalRandom random = java.util.concurrent.ThreadLocalRandom.current();
        for (int attempt = 0; attempt < 10; attempt++) {
            int digits = 6 + random.nextInt(5); // 6..10
            // digits=6: [100000, 999999]; digits=10: [1000000000, 9999999999] 共10位
            long min = (long) Math.pow(10, digits - 1);
            long max = (long) Math.pow(10, digits) - 1;
            // bound = max - min + 1，nextLong(bound) 返回 [0, bound) 然后 + min
            long bound = max - min + 1;
            long num = min + (bound <= 0 ? 0 : random.nextLong(bound));
            String groupNumber = String.valueOf(num);
            // 长度安全检查，防止越界（digits=10 时 long 上限 9,223,372,036,854,775,807，远大于 1e10）
            if (groupNumber.length() > 10) {
                groupNumber = groupNumber.substring(groupNumber.length() - 10);
                // 防止截取后首位为 0，丢弃重试
                if (groupNumber.charAt(0) == '0') continue;
            }
            LambdaQueryWrapper<GroupInfo> w = new LambdaQueryWrapper<>();
            w.eq(GroupInfo::getGroupNumber, groupNumber);
            if (groupMapper.selectCount(w) == 0) {
                return groupNumber;
            }
        }
        // 兜底：用时间戳生成保证唯一
        String fallback = String.valueOf(System.currentTimeMillis());
        return fallback.substring(Math.max(0, fallback.length() - 10));
    }

    @Override
    public GroupVO getGroupDetail(Long groupId, Long userId) {
        GroupInfo g = groupMapper.selectById(groupId);
        if (g == null) return null;
        return toGroupVO(g, userId);
    }

    @Override
    public Page<GroupVO> getMyGroups(Long userId, Integer page, Integer pageSize) {
        // 先通过 cms_group_member 查出用户加入的所有群组 ID
        LambdaQueryWrapper<GroupMember> memberWrapper = new LambdaQueryWrapper<>();
        memberWrapper.eq(GroupMember::getUserId, userId)
                .select(GroupMember::getGroupId);
        List<GroupMember> memberships = groupMemberMapper.selectList(memberWrapper);
        if (memberships.isEmpty()) {
            Page<GroupVO> empty = new Page<>(page, pageSize, 0L);
            empty.setRecords(new ArrayList<>());
            return empty;
        }
        List<Long> groupIds = memberships.stream()
                .map(GroupMember::getGroupId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        // 用 IN 条件从 cms_group_info 查群组列表
        LambdaQueryWrapper<GroupInfo> groupWrapper = new LambdaQueryWrapper<>();
        groupWrapper.in(GroupInfo::getId, groupIds)
                .eq(GroupInfo::getStatus, GroupInfo.STATUS_NORMAL)
                .orderByDesc(GroupInfo::getUpdatedAt);
        List<GroupInfo> groups = groupMapper.selectList(groupWrapper);
        List<GroupVO> vos = groups.stream().map(g -> toGroupVO(g, userId)).collect(Collectors.toList());

        // 手动分页
        int total = vos.size();
        int fromIndex = (page - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, total);
        List<GroupVO> pageList = fromIndex >= total ? new ArrayList<>() : vos.subList(fromIndex, toIndex);
        Page<GroupVO> result = new Page<>(page, pageSize, total);
        result.setRecords(pageList);
        return result;
    }

    @Override @Transactional
    public void joinGroup(Long userId, Long groupId) {
        GroupInfo g = groupMapper.selectById(groupId);
        if (g == null) throw new BusinessException(ErrorCode.NOT_FOUND, "群组不存在");
        LambdaQueryWrapper<GroupMember> w = new LambdaQueryWrapper<>();
        w.eq(GroupMember::getGroupId, groupId).eq(GroupMember::getUserId, userId);
        if (groupMemberMapper.selectCount(w) > 0) throw new BusinessException(ErrorCode.CONFLICT, "已在群中");
        GroupMember gm = new GroupMember();
        gm.setGroupId(groupId); gm.setUserId(userId); gm.setRole(0);
        groupMemberMapper.insert(gm);
        g.setMemberCount(g.getMemberCount() + 1);
        groupMapper.updateById(g);
    }

    @Override @Transactional
    public void leaveGroup(Long userId, Long groupId) {
        LambdaQueryWrapper<GroupMember> w = new LambdaQueryWrapper<>();
        w.eq(GroupMember::getGroupId, groupId).eq(GroupMember::getUserId, userId);
        groupMemberMapper.delete(w);
        GroupInfo g = groupMapper.selectById(groupId);
        if (g != null) { g.setMemberCount(Math.max(0, g.getMemberCount() - 1)); groupMapper.updateById(g); }
    }

    @Override @Transactional
    public void dismissGroup(Long userId, Long groupId) {
        GroupInfo g = groupMapper.selectById(groupId);
        if (g == null || !g.getOwnerId().equals(userId)) throw new BusinessException(ErrorCode.FORBIDDEN, "无权解散群组");
        g.setStatus(1); groupMapper.updateById(g);
    }

    @Override @Transactional
    public void inviteMembers(Long userId, GroupInviteRequest request) {
        GroupInfo g = groupMapper.selectById(request.getGroupId());
        if (g == null) throw new BusinessException(ErrorCode.NOT_FOUND, "群组不存在");
        for (Long uid : request.getUserIds()) {
            LambdaQueryWrapper<GroupMember> w = new LambdaQueryWrapper<>();
            w.eq(GroupMember::getGroupId, request.getGroupId()).eq(GroupMember::getUserId, uid);
            if (groupMemberMapper.selectCount(w) == 0) {
                GroupMember gm = new GroupMember();
                gm.setGroupId(request.getGroupId()); gm.setUserId(uid); gm.setRole(0);
                groupMemberMapper.insert(gm);
                g.setMemberCount(g.getMemberCount() + 1);
            }
        }
        groupMapper.updateById(g);
    }

    @Override @Transactional
    public void kickMember(Long ownerId, Long groupId, Long targetUserId) {
        GroupInfo g = groupMapper.selectById(groupId);
        if (g == null || !g.getOwnerId().equals(ownerId)) throw new BusinessException(ErrorCode.FORBIDDEN, "无权操作");
        LambdaQueryWrapper<GroupMember> w = new LambdaQueryWrapper<>();
        w.eq(GroupMember::getGroupId, groupId).eq(GroupMember::getUserId, targetUserId);
        groupMemberMapper.delete(w);
        g.setMemberCount(Math.max(0, g.getMemberCount() - 1)); groupMapper.updateById(g);
    }

    @Override @Transactional
    public void setAdmin(Long ownerId, Long groupId, Long targetUserId, boolean isAdmin) {
        GroupInfo g = groupMapper.selectById(groupId);
        if (g == null || !g.getOwnerId().equals(ownerId)) throw new BusinessException(ErrorCode.FORBIDDEN, "无权操作");
        LambdaQueryWrapper<GroupMember> w = new LambdaQueryWrapper<>();
        w.eq(GroupMember::getGroupId, groupId).eq(GroupMember::getUserId, targetUserId);
        GroupMember gm = groupMemberMapper.selectOne(w);
        if (gm != null) { gm.setRole(isAdmin ? 1 : 0); groupMemberMapper.updateById(gm); }
    }

    @Override @Transactional
    public void updateGroup(Long userId, Long groupId, String name, String avatar) {
        GroupInfo g = groupMapper.selectById(groupId);
        if (g == null) throw new BusinessException(ErrorCode.NOT_FOUND, "群组不存在");
        // 群主和管理员可修改
        LambdaQueryWrapper<GroupMember> w = new LambdaQueryWrapper<>();
        w.eq(GroupMember::getGroupId, groupId).eq(GroupMember::getUserId, userId);
        GroupMember gm = groupMemberMapper.selectOne(w);
        if (gm == null || gm.getRole() < 1) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅群主和管理员可修改群信息");
        }
        if (name != null && !name.isEmpty()) {
            g.setName(name);
        }
        if (avatar != null && !avatar.isEmpty()) {
            g.setAvatar(avatar);
        }
        groupMapper.updateById(g);
    }

    @Override
    public GroupMessageVO sendMessage(Long userId, GroupMessageRequest request) {
        // 校验发送者是否为群组成员
        if (!isMember(userId, request.getGroupId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "非群组成员无法发送消息");
        }
        String messageType = request.getMessageType() != null ? request.getMessageType() : "text";
        String content = request.getContent();

        // 仅对文本消息进行XSS过滤和敏感词检查
        if ("text".equals(messageType)) {
            content = HtmlWhitelistFilter.escapePlainText(content);
            if (sensitiveWordUtil.containsSensitiveWord(content)) {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, "消息包含敏感词，请修改后重试");
            }
        }

        GroupMessage msg = new GroupMessage();
        msg.setGroupId(request.getGroupId());
        msg.setSenderId(userId);
        msg.setContent(content);
        msg.setMessageType(messageType);
        // 处理@提及的用户ID列表
        if (request.getMentionedUserIds() != null && !request.getMentionedUserIds().isEmpty()) {
            msg.setMentionedUserIds(request.getMentionedUserIds().stream()
                    .map(String::valueOf).collect(Collectors.joining(",")));
        }
        // 填充发送者信息
        User sender = userMapper.selectById(userId);
        if (sender != null) {
            msg.setSenderName(sender.getNickname());
            msg.setSenderAvatar(sender.getAvatar());
        } else {
            msg.setSenderName("未知用户");
        }
        groupMessageMapper.insert(msg);
        GroupMessageVO msgVO = toMessageVO(msg);

        // 通过 WebSocket 广播消息给群组所有成员（HTTP 发送时也需要实时推送）
        try {
            String json = objectMapper.writeValueAsString(Map.of("type", "CHAT", "data", msgVO));
            GroupChatWebSocketHandler.broadcastToGroup(request.getGroupId(), json);
        } catch (Exception e) {
            log.warn("群消息WebSocket广播失败: groupId={}, error={}", request.getGroupId(), e.getMessage());
        }

        return msgVO;
    }

    @Override
    public List<GroupMessageVO> getMessages(Long groupId, Long userId, Long offset, int limit) {
        // 校验是否为群组成员
        if (!isMember(userId, groupId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "非群组成员无法查看消息");
        }
        LambdaQueryWrapper<GroupMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GroupMessage::getGroupId, groupId)
                .lt(GroupMessage::getId, offset > 0 ? offset : Long.MAX_VALUE)
                .orderByDesc(GroupMessage::getId)
                .last("LIMIT " + limit);
        List<GroupMessage> msgs = groupMessageMapper.selectList(wrapper);
        // 填充每条消息的发送者信息（transient字段未持久化）
        for (GroupMessage m : msgs) {
            if (m.getSenderName() == null || m.getSenderName().isEmpty()) {
                User s = userMapper.selectById(m.getSenderId());
                if (s != null) {
                    m.setSenderName(s.getNickname());
                    m.setSenderAvatar(s.getAvatar());
                }
            }
        }
        return msgs.stream().map(this::toMessageVO).collect(Collectors.toList());
    }

    @Override
    public List<GroupMemberVO> getMembers(Long groupId, Long userId) {
        if (!isMember(userId, groupId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "非群组成员无法查看成员列表");
        }
        LambdaQueryWrapper<GroupMember> w = new LambdaQueryWrapper<>();
        w.eq(GroupMember::getGroupId, groupId).orderByAsc(GroupMember::getRole).orderByAsc(GroupMember::getJoinedAt);
        List<GroupMember> members = groupMemberMapper.selectList(w);
        return members.stream().map(m -> {
            GroupMemberVO vo = new GroupMemberVO();
            vo.setId(m.getId());
            vo.setUserId(m.getUserId());
            vo.setGroupId(m.getGroupId());
            vo.setRole(m.getRole());
            vo.setNickname(m.getNickname());
            vo.setJoinedAt(m.getJoinedAt());
            User u = userMapper.selectById(m.getUserId());
            if (u != null) {
                vo.setUserName(u.getNickname());
                vo.setUserAvatar(u.getAvatar());
            }
            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 检查用户是否为群组成员
     */
    private boolean isMember(Long userId, Long groupId) {
        LambdaQueryWrapper<GroupMember> w = new LambdaQueryWrapper<>();
        w.eq(GroupMember::getGroupId, groupId).eq(GroupMember::getUserId, userId);
        return groupMemberMapper.selectCount(w) > 0;
    }

    @Override
    public List<GroupVO> searchGroups(String keyword, Integer page, Integer pageSize) {
        LambdaQueryWrapper<GroupInfo> w = new LambdaQueryWrapper<>();
        w.eq(GroupInfo::getStatus, 0).eq(GroupInfo::getIsPublic, 1)
            .and(w2 -> w2.like(GroupInfo::getName, keyword).or().like(GroupInfo::getGroupNumber, keyword));
        Page<GroupInfo> pg = new Page<>(page, pageSize);
        Page<GroupInfo> result = groupMapper.selectPage(pg, w);
        return result.getRecords().stream().map(g -> toGroupVO(g, null)).collect(Collectors.toList());
    }

    private GroupVO toGroupVO(GroupInfo g, Long userId) {
        GroupVO vo = new GroupVO(); vo.setId(g.getId()); vo.setName(g.getName());
        vo.setGroupNumber(g.getGroupNumber());
        vo.setAvatar(g.getAvatar()); vo.setDescription(g.getDescription());
        vo.setOwnerId(g.getOwnerId()); vo.setMemberCount(g.getMemberCount());
        vo.setMaxMembers(g.getMaxMembers()); vo.setIsPublic(g.getIsPublic());
        if (userId != null) {
            LambdaQueryWrapper<GroupMember> w = new LambdaQueryWrapper<>();
            w.eq(GroupMember::getGroupId, g.getId()).eq(GroupMember::getUserId, userId);
            GroupMember gm = groupMemberMapper.selectOne(w);
            if (gm != null) { vo.setMyRole(gm.getRole()); vo.setJoinedAt(gm.getJoinedAt()); }
        }
        return vo;
    }

    private GroupMessageVO toMessageVO(GroupMessage m) {
        GroupMessageVO vo = new GroupMessageVO(); vo.setId(m.getId());
        vo.setGroupId(m.getGroupId()); vo.setSenderId(m.getSenderId());
        vo.setSenderName(m.getSenderName()); vo.setSenderAvatar(m.getSenderAvatar());
        vo.setContent(m.getContent()); vo.setMessageType(m.getMessageType());
        vo.setCreatedAt(m.getCreatedAt());
        // 转换 mentionedUserIds: 逗号分隔字符串 → List<Long>
        if (m.getMentionedUserIds() != null && !m.getMentionedUserIds().isEmpty()) {
            try {
                vo.setMentionedUserIds(Arrays.stream(m.getMentionedUserIds().split(","))
                        .map(String::trim).filter(s -> !s.isEmpty())
                        .map(Long::parseLong).collect(Collectors.toList()));
            } catch (NumberFormatException e) {
                vo.setMentionedUserIds(new ArrayList<>());
            }
        } else {
            vo.setMentionedUserIds(new ArrayList<>());
        }
        return vo;
    }

    @Override
    public List<GroupMessageVO> searchMessages(Long groupId, Long userId, String keyword, String messageType,
                                                LocalDateTime startDate, LocalDateTime endDate, Long senderId,
                                                int offset, int limit) {
        if (!isMember(userId, groupId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "非群组成员无法搜索消息");
        }
        LambdaQueryWrapper<GroupMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GroupMessage::getGroupId, groupId);
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(GroupMessage::getContent, keyword);
        }
        if (messageType != null && !messageType.isEmpty()) {
            wrapper.eq(GroupMessage::getMessageType, messageType);
        }
        if (startDate != null) {
            wrapper.ge(GroupMessage::getCreatedAt, startDate);
        }
        if (endDate != null) {
            wrapper.le(GroupMessage::getCreatedAt, endDate.plusDays(1));
        }
        if (senderId != null) {
            wrapper.eq(GroupMessage::getSenderId, senderId);
        }
        wrapper.orderByDesc(GroupMessage::getId)
                .last("LIMIT " + limit + " OFFSET " + offset);
        List<GroupMessage> msgs = groupMessageMapper.selectList(wrapper);
        for (GroupMessage m : msgs) {
            if (m.getSenderName() == null || m.getSenderName().isEmpty()) {
                User s = userMapper.selectById(m.getSenderId());
                if (s != null) {
                    m.setSenderName(s.getNickname());
                    m.setSenderAvatar(s.getAvatar());
                }
            }
        }
        return msgs.stream().map(this::toMessageVO).collect(Collectors.toList());
    }

    @Override
    public GroupMessageVO sendAIMessage(Long groupId, Long userId, String question) {
        if (!isMember(userId, groupId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "非群组成员无法使用AI助手");
        }

        // 异步生成AI回复，不阻塞HTTP请求
        CompletableFuture.runAsync(() -> {
            try {
                // 调用AI服务
                AIWriteRequest aiReq = new AIWriteRequest();
                aiReq.setPrompt(question);
                aiReq.setMode("chat");
                AIResponseVO aiResp = aiService.generateText(aiReq);

                // 将AI回复持久化为群组消息
                GroupMessage msg = new GroupMessage();
                msg.setGroupId(groupId);
                msg.setSenderId(0L); // AI机器人
                msg.setSenderName("AI助手");
                msg.setSenderAvatar("");
                msg.setContent(aiResp.getContent());
                msg.setMessageType("ai_reply");
                groupMessageMapper.insert(msg);

                // 通过WebSocket广播AI回复给所有群组成员
                GroupMessageVO vo = toMessageVO(msg);
                String wsPayload = objectMapper.writeValueAsString(
                        Map.of("type", "CHAT", "data", vo)
                );
                GroupChatWebSocketHandler.broadcastAIMessage(groupId, wsPayload);

                log.info("AI回复已生成并广播: groupId={}, msgId={}", groupId, msg.getId());
            } catch (Exception e) {
                log.error("异步生成AI回复失败: groupId={}, userId={}, error={}", groupId, userId, e.getMessage(), e);
            }
        });

        // 立即返回null，AI回复将通过WebSocket推送
        return null;
    }

    @Override @Transactional
    public void requestJoin(Long userId, Long groupId, String message) {
        GroupInfo g = groupMapper.selectById(groupId);
        if (g == null) throw new BusinessException(ErrorCode.NOT_FOUND, "群组不存在");
        if (g.getStatus() == GroupInfo.STATUS_DISMISSED) throw new BusinessException(ErrorCode.BUSINESS_ERROR, "群组已解散");
        // 已经是成员
        if (isMember(userId, groupId)) {
            throw new BusinessException(ErrorCode.CONFLICT, "您已是该群成员");
        }
        // 检查是否有待审批的申请
        LambdaQueryWrapper<GroupJoinRequest> w = new LambdaQueryWrapper<>();
        w.eq(GroupJoinRequest::getGroupId, groupId)
         .eq(GroupJoinRequest::getUserId, userId)
         .eq(GroupJoinRequest::getStatus, GroupJoinRequest.STATUS_PENDING);
        if (groupJoinRequestMapper.selectCount(w) > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "您已提交过入群申请，请等待审批");
        }
        GroupJoinRequest req = new GroupJoinRequest();
        req.setGroupId(groupId);
        req.setUserId(userId);
        req.setMessage(message != null ? message : "");
        req.setStatus(GroupJoinRequest.STATUS_PENDING);
        req.setCreatedAt(java.time.LocalDateTime.now());
        groupJoinRequestMapper.insert(req);
    }

    @Override @Transactional
    public void approveJoinRequest(Long ownerId, Long requestId) {
        GroupJoinRequest req = groupJoinRequestMapper.selectById(requestId);
        if (req == null) throw new BusinessException(ErrorCode.NOT_FOUND, "申请不存在");
        if (req.getStatus() != GroupJoinRequest.STATUS_PENDING) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "该申请已被处理");
        }
        GroupInfo g = groupMapper.selectById(req.getGroupId());
        if (g == null || !g.getOwnerId().equals(ownerId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权审批入群申请");
        }
        // 更新申请状态
        req.setStatus(GroupJoinRequest.STATUS_APPROVED);
        groupJoinRequestMapper.updateById(req);
        // 添加为群成员
        if (!isMember(req.getUserId(), req.getGroupId())) {
            GroupMember gm = new GroupMember();
            gm.setGroupId(req.getGroupId());
            gm.setUserId(req.getUserId());
            gm.setRole(GroupMember.ROLE_MEMBER);
            gm.setJoinedAt(java.time.LocalDateTime.now());
            groupMemberMapper.insert(gm);
            g.setMemberCount(g.getMemberCount() + 1);
            groupMapper.updateById(g);
        }
    }

    @Override @Transactional
    public void rejectJoinRequest(Long ownerId, Long requestId) {
        GroupJoinRequest req = groupJoinRequestMapper.selectById(requestId);
        if (req == null) throw new BusinessException(ErrorCode.NOT_FOUND, "申请不存在");
        if (req.getStatus() != GroupJoinRequest.STATUS_PENDING) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "该申请已被处理");
        }
        GroupInfo g = groupMapper.selectById(req.getGroupId());
        if (g == null || !g.getOwnerId().equals(ownerId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权审批入群申请");
        }
        req.setStatus(GroupJoinRequest.STATUS_REJECTED);
        groupJoinRequestMapper.updateById(req);
    }

    @Override
    public List<GroupJoinRequestVO> getPendingRequests(Long groupId, Long userId) {
        GroupInfo g = groupMapper.selectById(groupId);
        if (g == null) throw new BusinessException(ErrorCode.NOT_FOUND, "群组不存在");
        // 只有群主和管理员可以查看待审批列表
        if (!g.getOwnerId().equals(userId)) {
            LambdaQueryWrapper<GroupMember> mw = new LambdaQueryWrapper<>();
            mw.eq(GroupMember::getGroupId, groupId).eq(GroupMember::getUserId, userId);
            GroupMember member = groupMemberMapper.selectOne(mw);
            if (member == null || member.getRole() < GroupMember.ROLE_ADMIN) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "无权查看入群申请");
            }
        }
        LambdaQueryWrapper<GroupJoinRequest> w = new LambdaQueryWrapper<>();
        w.eq(GroupJoinRequest::getGroupId, groupId)
         .eq(GroupJoinRequest::getStatus, GroupJoinRequest.STATUS_PENDING)
         .orderByDesc(GroupJoinRequest::getCreatedAt);
        List<GroupJoinRequest> requests = groupJoinRequestMapper.selectList(w);
        return requests.stream().map(r -> {
            GroupJoinRequestVO vo = new GroupJoinRequestVO();
            vo.setId(r.getId());
            vo.setGroupId(r.getGroupId());
            vo.setUserId(r.getUserId());
            vo.setMessage(r.getMessage());
            vo.setStatus(r.getStatus());
            vo.setCreatedAt(r.getCreatedAt());
            User u = userMapper.selectById(r.getUserId());
            if (u != null) {
                vo.setUserName(u.getNickname());
                vo.setUserAvatar(u.getAvatar());
            }
            vo.setGroupName(g.getName());
            return vo;
        }).collect(Collectors.toList());
    }
}