package com.zhixun.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhixun.dto.group.GroupCreateRequest;
import com.zhixun.dto.group.GroupInviteRequest;
import com.zhixun.dto.group.GroupMessageRequest;
import com.zhixun.vo.GroupMessageVO;
import com.zhixun.vo.GroupVO;

import java.util.List;

public interface GroupService {
    Long createGroup(Long userId, GroupCreateRequest request);
    GroupVO getGroupDetail(Long groupId, Long userId);
    Page<GroupVO> getMyGroups(Long userId, Integer page, Integer pageSize);
    void joinGroup(Long userId, Long groupId);
    void leaveGroup(Long userId, Long groupId);
    void dismissGroup(Long userId, Long groupId);
    void inviteMembers(Long userId, GroupInviteRequest request);
    void kickMember(Long ownerId, Long groupId, Long targetUserId);
    void setAdmin(Long ownerId, Long groupId, Long targetUserId, boolean isAdmin);
    GroupMessageVO sendMessage(Long userId, GroupMessageRequest request);
    List<GroupMessageVO> getMessages(Long groupId, Long userId, Long offset, int limit);
    List<GroupVO> searchGroups(String keyword, Integer page, Integer pageSize);
}