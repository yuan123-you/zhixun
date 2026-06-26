package com.zhixun.controller;

import com.zhixun.common.result.PageResult;
import com.zhixun.common.result.R;
import com.zhixun.common.util.SecurityUtil;
import com.zhixun.dto.group.GroupCreateRequest;
import com.zhixun.dto.group.GroupInviteRequest;
import com.zhixun.dto.group.GroupMessageRequest;
import com.zhixun.service.GroupService;
import com.zhixun.vo.GroupMessageVO;
import com.zhixun.vo.GroupVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/v1/groups")
@RequiredArgsConstructor
public class GroupController {
    private final GroupService groupService;
    private final SecurityUtil securityUtil;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public R<Long> create(@Valid @RequestBody GroupCreateRequest request) {
        return R.ok(groupService.createGroup(securityUtil.getCurrentUserId(), request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public R<GroupVO> detail(@PathVariable Long id) {
        return R.ok(groupService.getGroupDetail(id, securityUtil.getCurrentUserId()));
    }

    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public R<PageResult<GroupVO>> myGroups(@RequestParam(defaultValue = "1") Integer page,
                                            @RequestParam(defaultValue = "20") Integer pageSize) {
        Page<GroupVO> result = groupService.getMyGroups(securityUtil.getCurrentUserId(), page, pageSize);
        return R.ok(new PageResult<>(result.getRecords(), result.getTotal(), page, pageSize));
    }

    @PostMapping("/{id}/join")
    @PreAuthorize("isAuthenticated()")
    public R<Void> join(@PathVariable Long id) {
        groupService.joinGroup(securityUtil.getCurrentUserId(), id);
        return R.ok();
    }

    @PostMapping("/{id}/leave")
    @PreAuthorize("isAuthenticated()")
    public R<Void> leave(@PathVariable Long id) {
        groupService.leaveGroup(securityUtil.getCurrentUserId(), id);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public R<Void> dismiss(@PathVariable Long id) {
        groupService.dismissGroup(securityUtil.getCurrentUserId(), id);
        return R.ok();
    }

    @PostMapping("/invite")
    @PreAuthorize("isAuthenticated()")
    public R<Void> invite(@Valid @RequestBody GroupInviteRequest request) {
        groupService.inviteMembers(securityUtil.getCurrentUserId(), request);
        return R.ok();
    }

    @PostMapping("/{groupId}/kick/{userId}")
    @PreAuthorize("isAuthenticated()")
    public R<Void> kick(@PathVariable Long groupId, @PathVariable Long userId) {
        groupService.kickMember(securityUtil.getCurrentUserId(), groupId, userId);
        return R.ok();
    }

    @PostMapping("/{groupId}/admin/{userId}")
    @PreAuthorize("isAuthenticated()")
    public R<Void> setAdmin(@PathVariable Long groupId, @PathVariable Long userId, @RequestParam(defaultValue = "true") boolean isAdmin) {
        groupService.setAdmin(securityUtil.getCurrentUserId(), groupId, userId, isAdmin);
        return R.ok();
    }

    @GetMapping("/{groupId}/messages")
    @PreAuthorize("isAuthenticated()")
    public R<List<GroupMessageVO>> messages(@PathVariable Long groupId,
                                             @RequestParam(defaultValue = "0") Long offset,
                                             @RequestParam(defaultValue = "50") int limit) {
        return R.ok(groupService.getMessages(groupId, securityUtil.getCurrentUserId(), offset, limit));
    }

    @PostMapping("/messages")
    @PreAuthorize("isAuthenticated()")
    public R<GroupMessageVO> sendMessage(@Valid @RequestBody GroupMessageRequest request) {
        return R.ok(groupService.sendMessage(securityUtil.getCurrentUserId(), request));
    }

    @GetMapping("/search")
    public R<PageResult<GroupVO>> search(@RequestParam String keyword,
                                          @RequestParam(defaultValue = "1") Integer page,
                                          @RequestParam(defaultValue = "20") Integer pageSize) {
        List<GroupVO> list = groupService.searchGroups(keyword, page, pageSize);
        return R.ok(new PageResult<>(list, (long) list.size(), page, pageSize));
    }
}