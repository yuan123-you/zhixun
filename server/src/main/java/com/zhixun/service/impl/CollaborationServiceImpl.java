package com.zhixun.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhixun.dto.collaboration.CollaboratorInviteRequest;
import com.zhixun.dto.collaboration.CollaboratorRespondRequest;
import com.zhixun.entity.Article;
import com.zhixun.entity.ArticleCollaborator;
import com.zhixun.mapper.ArticleCollaboratorMapper;
import com.zhixun.mapper.ArticleMapper;
import com.zhixun.service.CollaborationService;
import com.zhixun.vo.CollaboratorVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CollaborationServiceImpl implements CollaborationService {
    private final ArticleCollaboratorMapper articleCollaboratorMapper;
    private final ArticleMapper articleMapper;

    @Override @Transactional
    public void inviteCollaborator(Long inviterId, CollaboratorInviteRequest request) {
        Article article = articleMapper.selectById(request.getArticleId());
        if (article == null) throw new RuntimeException("Article not found");
        if (!article.getAuthorId().equals(inviterId)) throw new RuntimeException("Only author can invite");
        LambdaQueryWrapper<ArticleCollaborator> w = new LambdaQueryWrapper<>();
        w.eq(ArticleCollaborator::getArticleId, request.getArticleId())
         .eq(ArticleCollaborator::getUserId, request.getUserId());
        if (articleCollaboratorMapper.selectCount(w) > 0) throw new RuntimeException("Already invited");
        ArticleCollaborator ac = new ArticleCollaborator();
        ac.setArticleId(request.getArticleId()); ac.setUserId(request.getUserId());
        ac.setPermission(request.getPermission()); ac.setInvitedBy(inviterId);
        articleCollaboratorMapper.insert(ac);
    }

    @Override @Transactional
    public void respondInvitation(Long userId, Long collaboratorId, CollaboratorRespondRequest request) {
        ArticleCollaborator ac = articleCollaboratorMapper.selectById(collaboratorId);
        if (ac == null || !ac.getUserId().equals(userId)) throw new RuntimeException("Invalid invitation");
        ac.setStatus(request.getStatus());
        articleCollaboratorMapper.updateById(ac);
    }

    @Override
    public List<CollaboratorVO> getArticleCollaborators(Long articleId) {
        LambdaQueryWrapper<ArticleCollaborator> w = new LambdaQueryWrapper<>();
        w.eq(ArticleCollaborator::getArticleId, articleId).eq(ArticleCollaborator::getStatus, 1);
        List<ArticleCollaborator> list = articleCollaboratorMapper.selectList(w);
        return list.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public List<CollaboratorVO> getMyInvitations(Long userId) {
        LambdaQueryWrapper<ArticleCollaborator> w = new LambdaQueryWrapper<>();
        w.eq(ArticleCollaborator::getUserId, userId).eq(ArticleCollaborator::getStatus, 0);
        List<ArticleCollaborator> list = articleCollaboratorMapper.selectList(w);
        return list.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override @Transactional
    public void removeCollaborator(Long ownerId, Long articleId, Long userId) {
        Article article = articleMapper.selectById(articleId);
        if (article == null || !article.getAuthorId().equals(ownerId)) throw new RuntimeException("Permission denied");
        LambdaQueryWrapper<ArticleCollaborator> w = new LambdaQueryWrapper<>();
        w.eq(ArticleCollaborator::getArticleId, articleId).eq(ArticleCollaborator::getUserId, userId);
        articleCollaboratorMapper.delete(w);
    }

    @Override
    public boolean canEditArticle(Long userId, Long articleId) {
        Article article = articleMapper.selectById(articleId);
        if (article != null && article.getAuthorId().equals(userId)) return true;
        LambdaQueryWrapper<ArticleCollaborator> w = new LambdaQueryWrapper<>();
        w.eq(ArticleCollaborator::getArticleId, articleId).eq(ArticleCollaborator::getUserId, userId)
         .eq(ArticleCollaborator::getStatus, 1).eq(ArticleCollaborator::getPermission, "edit");
        return articleCollaboratorMapper.selectCount(w) > 0;
    }

    private CollaboratorVO toVO(ArticleCollaborator ac) {
        CollaboratorVO vo = new CollaboratorVO(); vo.setId(ac.getId()); vo.setArticleId(ac.getArticleId());
        vo.setUserId(ac.getUserId()); vo.setUserName(ac.getUserName()); vo.setUserAvatar(ac.getUserAvatar());
        vo.setPermission(ac.getPermission()); vo.setStatus(ac.getStatus()); vo.setCreatedAt(ac.getCreatedAt());
        return vo;
    }
}
