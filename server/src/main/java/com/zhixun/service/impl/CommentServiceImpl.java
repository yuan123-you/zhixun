package com.zhixun.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhixun.common.exception.BusinessException;
import com.zhixun.common.result.ErrorCode;
import com.zhixun.common.result.PageResult;
import com.zhixun.common.util.SensitiveWordUtil;
import com.zhixun.dto.comment.CommentCreateRequest;
import com.zhixun.entity.Article;
import com.zhixun.entity.Comment;
import com.zhixun.entity.User;
import com.zhixun.enums.CommentStatusEnum;
import com.zhixun.mapper.ArticleMapper;
import com.zhixun.mapper.CommentMapper;
import com.zhixun.mapper.UserMapper;
import com.zhixun.service.CommentService;
import com.zhixun.vo.CommentVO;
import com.zhixun.vo.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 评论服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentMapper commentMapper;
    private final ArticleMapper articleMapper;
    private final UserMapper userMapper;
    private final SensitiveWordUtil sensitiveWordUtil;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createComment(Long userId, CommentCreateRequest request) {
        // 检查文章是否存在
        Article article = articleMapper.selectById(request.getArticleId());
        if (article == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "文章不存在");
        }

        // 敏感词检测
        if (sensitiveWordUtil.containsSensitiveWord(request.getContent())) {
            throw new BusinessException(ErrorCode.SENSITIVE_WORD_DETECTED, "评论内容包含敏感词");
        }

        // 如果有父评论，校验父评论是否存在
        if (request.getParentId() != null && request.getParentId() > 0) {
            Comment parentComment = commentMapper.selectById(request.getParentId());
            if (parentComment == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "父评论不存在");
            }
        }

        // 创建评论
        Comment comment = new Comment();
        comment.setArticleId(request.getArticleId());
        comment.setUserId(userId);
        comment.setParentId(request.getParentId() != null ? request.getParentId() : 0L);
        comment.setReplyToId(request.getReplyUserId());
        comment.setContent(request.getContent());
        comment.setStatus(CommentStatusEnum.NORMAL);
        comment.setLikeCount(0);

        commentMapper.insert(comment);

        // 增加文章评论数（SQL 原子操作）
        articleMapper.update(null, new LambdaUpdateWrapper<Article>()
                .eq(Article::getId, request.getArticleId())
                .setSql("comment_count = comment_count + 1"));

        return comment.getId();
    }

    @Override
    public PageResult<CommentVO> getComments(Long articleId, String sort, Integer page, Integer pageSize) {
        // 查询顶级评论（parentId = 0）
        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Comment::getArticleId, articleId)
                .eq(Comment::getParentId, 0L)
                .eq(Comment::getStatus, CommentStatusEnum.NORMAL);

        // 排序方式
        if ("hot".equals(sort)) {
            wrapper.orderByDesc(Comment::getLikeCount).orderByDesc(Comment::getCreatedAt);
        } else {
            // 默认最新排序
            wrapper.orderByDesc(Comment::getCreatedAt);
        }

        Page<Comment> commentPage = new Page<>(page, pageSize);
        Page<Comment> result = commentMapper.selectPage(commentPage, wrapper);

        // 转换为 VO（含子评论）
        List<CommentVO> voList = convertToVOListWithChildren(result.getRecords(), articleId);

        return new PageResult<>(voList, result.getTotal(), page, pageSize);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteComment(Long userId, Long commentId) {
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "评论不存在");
        }

        // 权限校验：仅评论者本人或管理员可删除
        if (!comment.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权删除此评论");
        }

        // 软删除：更新状态为已删除
        comment.setStatus(CommentStatusEnum.DELETED);
        commentMapper.updateById(comment);

        // 减少文章评论数（SQL 原子操作）
        articleMapper.update(null, new LambdaUpdateWrapper<Article>()
                .eq(Article::getId, comment.getArticleId())
                .gt(Article::getCommentCount, 0)
                .setSql("comment_count = comment_count - 1"));
    }

    @Override
    public PageResult<CommentVO> getMyComments(Long userId, Integer page, Integer pageSize) {
        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Comment::getUserId, userId)
                .eq(Comment::getStatus, CommentStatusEnum.NORMAL)
                .orderByDesc(Comment::getCreatedAt);

        Page<Comment> commentPage = new Page<>(page, pageSize);
        Page<Comment> result = commentMapper.selectPage(commentPage, wrapper);

        List<CommentVO> voList = convertToVOList(result.getRecords());

        return new PageResult<>(voList, result.getTotal(), page, pageSize);
    }

    // ========== 内部方法 ==========

    /**
     * 将评论列表转换为 VO 列表（含子评论嵌套）
     */
    private List<CommentVO> convertToVOListWithChildren(List<Comment> comments, Long articleId) {
        if (CollectionUtils.isEmpty(comments)) {
            return Collections.emptyList();
        }

        // 获取所有顶级评论ID
        List<Long> parentIds = comments.stream().map(Comment::getId).collect(Collectors.toList());

        // 查询所有子评论
        LambdaQueryWrapper<Comment> childWrapper = new LambdaQueryWrapper<>();
        childWrapper.eq(Comment::getArticleId, articleId)
                .in(Comment::getParentId, parentIds)
                .ne(Comment::getParentId, 0L)
                .eq(Comment::getStatus, CommentStatusEnum.NORMAL)
                .orderByAsc(Comment::getCreatedAt);
        List<Comment> allChildren = commentMapper.selectList(childWrapper);

        // 收集所有需要查询的用户ID
        List<Comment> allComments = new java.util.ArrayList<>(comments);
        allComments.addAll(allChildren);
        List<Long> userIds = allComments.stream()
                .map(Comment::getUserId)
                .distinct()
                .collect(Collectors.toList());
        List<Long> replyUserIds = allComments.stream()
                .map(Comment::getReplyToId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        userIds.addAll(replyUserIds);

        Map<Long, User> userMap = CollectionUtils.isEmpty(userIds) ? Collections.emptyMap()
                : userMapper.selectBatchIds(userIds.stream().distinct().collect(Collectors.toList())).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        // 按父评论ID分组子评论
        Map<Long, List<Comment>> childrenMap = allChildren.stream()
                .collect(Collectors.groupingBy(Comment::getParentId));

        // 转换顶级评论
        return comments.stream().map(comment -> {
            CommentVO vo = buildCommentVO(comment, userMap);

            // 设置子评论
            List<Comment> children = childrenMap.getOrDefault(comment.getId(), Collections.emptyList());
            List<CommentVO> childVOs = children.stream()
                    .map(child -> buildCommentVO(child, userMap))
                    .collect(Collectors.toList());
            vo.setChildren(childVOs);

            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 将评论列表转换为 VO 列表（不含子评论嵌套）
     */
    private List<CommentVO> convertToVOList(List<Comment> comments) {
        if (CollectionUtils.isEmpty(comments)) {
            return Collections.emptyList();
        }

        List<Long> userIds = comments.stream()
                .map(Comment::getUserId)
                .distinct()
                .collect(Collectors.toList());
        List<Long> replyUserIds = comments.stream()
                .map(Comment::getReplyToId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        userIds.addAll(replyUserIds);

        Map<Long, User> userMap = CollectionUtils.isEmpty(userIds) ? Collections.emptyMap()
                : userMapper.selectBatchIds(userIds.stream().distinct().collect(Collectors.toList())).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        return comments.stream()
                .map(comment -> buildCommentVO(comment, userMap))
                .collect(Collectors.toList());
    }

    /**
     * 构建评论 VO
     */
    private CommentVO buildCommentVO(Comment comment, Map<Long, User> userMap) {
        CommentVO vo = new CommentVO();
        vo.setId(comment.getId());
        vo.setArticleId(comment.getArticleId());
        vo.setContent(comment.getContent());
        vo.setStatus(comment.getStatus() != null ? comment.getStatus().getValue() : null);
        vo.setLikeCount(comment.getLikeCount());
        vo.setParentId(comment.getParentId());
        vo.setCreatedAt(comment.getCreatedAt());

        // 设置评论用户信息
        User user = userMap.get(comment.getUserId());
        if (user != null) {
            vo.setUser(buildUserVO(user));
        }

        // 设置回复目标用户信息
        if (comment.getReplyToId() != null) {
            User replyUser = userMap.get(comment.getReplyToId());
            if (replyUser != null) {
                vo.setReplyUser(buildUserVO(replyUser));
            }
        }

        return vo;
    }

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
