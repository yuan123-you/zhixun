package com.zhixun.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhixun.common.exception.BusinessException;
import com.zhixun.common.result.ErrorCode;
import com.zhixun.entity.GroupInfo;
import com.zhixun.entity.GroupMember;
import com.zhixun.mapper.GroupMapper;
import com.zhixun.mapper.GroupMemberMapper;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Same normal-group/member rule for queued AI work and live chat subscription/delivery. */
@Service
@RequiredArgsConstructor
public class GroupChatAccess {
    private final GroupMapper groups;
    private final GroupMemberMapper members;

    private GroupInfo normalGroup(Long groupId) {
        GroupInfo group = groups.selectById(groupId);
        if (group == null) throw new BusinessException(ErrorCode.NOT_FOUND, "群组不存在");
        if (!Objects.equals(group.getStatus(), GroupInfo.STATUS_NORMAL)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "群组已解散或不可用");
        }
        return group;
    }

    public void requireMember(Long groupId, Long userId) {
        normalGroup(groupId);
        if (userId == null || members.selectCount(new LambdaQueryWrapper<GroupMember>()
                .eq(GroupMember::getGroupId, groupId).eq(GroupMember::getUserId, userId)) <= 0) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "非群组成员无法访问群聊");
        }
    }

    public Set<Long> currentRecipients(Long groupId) {
        normalGroup(groupId);
        return members.selectList(new LambdaQueryWrapper<GroupMember>().eq(GroupMember::getGroupId, groupId))
                .stream().map(GroupMember::getUserId).filter(Objects::nonNull).collect(Collectors.toSet());
    }
}
