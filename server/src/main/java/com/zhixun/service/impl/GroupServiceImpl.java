package com.zhixun.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhixun.dto.group.GroupCreateRequest;
import com.zhixun.dto.group.GroupInviteRequest;
import com.zhixun.dto.group.GroupMessageRequest;
import com.zhixun.entity.*;
import com.zhixun.mapper.*;
import com.zhixun.service.GroupService;
import com.zhixun.vo.GroupMessageVO;
import com.zhixun.vo.GroupVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GroupServiceImpl implements GroupService {
    private final GroupMapper groupMapper;
    private final GroupMemberMapper groupMemberMapper;
    private final GroupMessageMapper groupMessageMapper;
    private final UserMapper userMapper;

    @Override @Transactional
    public Long createGroup(Long userId, GroupCreateRequest request) {
        GroupInfo g = new GroupInfo();
        g.setName(request.getName());
        g.setDescription(request.getDescription());
        g.setAvatar(request.getAvatar());
        g.setOwnerId(userId);
        g.setIsPublic(request.getIsPublic() != null ? request.getIsPublic() : 1);
        groupMapper.insert(g);
        GroupMember gm = new GroupMember();
        gm.setGroupId(g.getId()); gm.setUserId(userId);
        gm.setRole(GroupMember.ROLE_OWNER);
        groupMemberMapper.insert(gm);
        return g.getId();
    }

    @Override
    public GroupVO getGroupDetail(Long groupId, Long userId) {
        GroupInfo g = groupMapper.selectById(groupId);
        if (g == null) return null;
        return toGroupVO(g, userId);
    }

    @Override
    public Page<GroupVO> getMyGroups(Long userId, Integer page, Integer pageSize) {
        List<GroupInfo> groups = groupMapper.selectByUserId(userId);
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
        if (g == null) throw new RuntimeException("群组不存在");
        LambdaQueryWrapper<GroupMember> w = new LambdaQueryWrapper<>();
        w.eq(GroupMember::getGroupId, groupId).eq(GroupMember::getUserId, userId);
        if (groupMemberMapper.selectCount(w) > 0) throw new RuntimeException("已在群中");
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
        if (g == null || !g.getOwnerId().equals(userId)) throw new RuntimeException("鏃犳潈瑙ｆ暎缇ょ粍");
        g.setStatus(1); groupMapper.updateById(g);
    }

    @Override @Transactional
    public void inviteMembers(Long userId, GroupInviteRequest request) {
        GroupInfo g = groupMapper.selectById(request.getGroupId());
        if (g == null) throw new RuntimeException("群组不存在");
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
        if (g == null || !g.getOwnerId().equals(ownerId)) throw new RuntimeException("鏃犳潈鎿嶄綔");
        LambdaQueryWrapper<GroupMember> w = new LambdaQueryWrapper<>();
        w.eq(GroupMember::getGroupId, groupId).eq(GroupMember::getUserId, targetUserId);
        groupMemberMapper.delete(w);
        g.setMemberCount(Math.max(0, g.getMemberCount() - 1)); groupMapper.updateById(g);
    }

    @Override @Transactional
    public void setAdmin(Long ownerId, Long groupId, Long targetUserId, boolean isAdmin) {
        GroupInfo g = groupMapper.selectById(groupId);
        if (g == null || !g.getOwnerId().equals(ownerId)) throw new RuntimeException("鏃犳潈鎿嶄綔");
        LambdaQueryWrapper<GroupMember> w = new LambdaQueryWrapper<>();
        w.eq(GroupMember::getGroupId, groupId).eq(GroupMember::getUserId, targetUserId);
        GroupMember gm = groupMemberMapper.selectOne(w);
        if (gm != null) { gm.setRole(isAdmin ? 1 : 0); groupMemberMapper.updateById(gm); }
    }

    @Override
    public GroupMessageVO sendMessage(Long userId, GroupMessageRequest request) {
        GroupMessage msg = new GroupMessage();
        msg.setGroupId(request.getGroupId()); msg.setSenderId(userId);
        msg.setContent(request.getContent());
        msg.setMessageType(request.getMessageType() != null ? request.getMessageType() : "text");
        groupMessageMapper.insert(msg);
        return toMessageVO(msg);
    }

    @Override
    public List<GroupMessageVO> getMessages(Long groupId, Long userId, Long offset, int limit) {
        List<GroupMessage> msgs = groupMessageMapper.selectByGroupId(groupId, offset, limit);
        return msgs.stream().map(this::toMessageVO).collect(Collectors.toList());
    }

    @Override
    public List<GroupVO> searchGroups(String keyword, Integer page, Integer pageSize) {
        LambdaQueryWrapper<GroupInfo> w = new LambdaQueryWrapper<>();
        w.eq(GroupInfo::getStatus, 0).eq(GroupInfo::getIsPublic, 1).like(GroupInfo::getName, keyword);
        Page<GroupInfo> pg = new Page<>(page, pageSize);
        Page<GroupInfo> result = groupMapper.selectPage(pg, w);
        return result.getRecords().stream().map(g -> toGroupVO(g, null)).collect(Collectors.toList());
    }

    private GroupVO toGroupVO(GroupInfo g, Long userId) {
        GroupVO vo = new GroupVO(); vo.setId(g.getId()); vo.setName(g.getName());
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
        vo.setCreatedAt(m.getCreatedAt()); return vo;
    }
}