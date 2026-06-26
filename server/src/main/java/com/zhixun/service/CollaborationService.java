package com.zhixun.service;

import com.zhixun.dto.collaboration.CollaboratorInviteRequest;
import com.zhixun.dto.collaboration.CollaboratorRespondRequest;
import com.zhixun.vo.CollaboratorVO;

import java.util.List;

public interface CollaborationService {
    void inviteCollaborator(Long inviterId, CollaboratorInviteRequest request);
    void respondInvitation(Long userId, Long collaboratorId, CollaboratorRespondRequest request);
    List<CollaboratorVO> getArticleCollaborators(Long articleId);
    List<CollaboratorVO> getMyInvitations(Long userId);
    void removeCollaborator(Long ownerId, Long articleId, Long userId);
    boolean canEditArticle(Long userId, Long articleId);
}