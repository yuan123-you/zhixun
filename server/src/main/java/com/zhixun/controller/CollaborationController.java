package com.zhixun.controller;

import com.zhixun.common.result.R;
import com.zhixun.common.util.SecurityUtil;
import com.zhixun.dto.collaboration.CollaboratorInviteRequest;
import com.zhixun.dto.collaboration.CollaboratorRespondRequest;
import com.zhixun.service.CollaborationService;
import com.zhixun.vo.CollaboratorVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/v1/collaborations")
@RequiredArgsConstructor
public class CollaborationController {
    private final CollaborationService collaborationService;
    private final SecurityUtil securityUtil;

    @PostMapping("/invite")
    @PreAuthorize("isAuthenticated()")
    public R<Void> invite(@Valid @RequestBody CollaboratorInviteRequest request) {
        collaborationService.inviteCollaborator(securityUtil.getCurrentUserId(), request);
        return R.ok();
    }

    @PutMapping("/{id}/respond")
    @PreAuthorize("isAuthenticated()")
    public R<Void> respond(@PathVariable Long id, @Valid @RequestBody CollaboratorRespondRequest request) {
        collaborationService.respondInvitation(securityUtil.getCurrentUserId(), id, request);
        return R.ok();
    }

    @GetMapping("/article/{articleId}")
    public R<List<CollaboratorVO>> articleCollaborators(@PathVariable Long articleId) {
        return R.ok(collaborationService.getArticleCollaborators(articleId));
    }

    @GetMapping("/invitations")
    @PreAuthorize("isAuthenticated()")
    public R<List<CollaboratorVO>> myInvitations() {
        return R.ok(collaborationService.getMyInvitations(securityUtil.getCurrentUserId()));
    }

    @DeleteMapping("/{articleId}/user/{userId}")
    @PreAuthorize("isAuthenticated()")
    public R<Void> removeCollaborator(@PathVariable Long articleId, @PathVariable Long userId) {
        collaborationService.removeCollaborator(securityUtil.getCurrentUserId(), articleId, userId);
        return R.ok();
    }

    @GetMapping("/can-edit/{articleId}")
    @PreAuthorize("isAuthenticated()")
    public R<Boolean> canEdit(@PathVariable Long articleId) {
        return R.ok(collaborationService.canEditArticle(securityUtil.getCurrentUserId(), articleId));
    }
}