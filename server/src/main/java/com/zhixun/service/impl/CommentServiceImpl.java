package com.zhixun.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhixun.common.exception.BusinessException;
import com.zhixun.common.result.ErrorCode;
import com.zhixun.common.result.PageResult;
import com.zhixun.common.util.SensitiveWordUtil;
import com.zhixun.common.util.SecurityUtil;
import com.zhixun.config.Slave;
import com.zhixun.dto.comment.CommentCreateRequest;
import com.zhixun.dto.comment.CommentReportRequest;
import com.zhixun.entity.Article;
import com.zhixun.entity.ArticleLike;
import com.zhixun.entity.Comment;
import com.zhixun.entity.CommentReport;
import com.zhixun.entity.User;
import com.zhixun.enums.CommentStatusEnum;
import com.zhixun.enums.LikeTargetTypeEnum;
import com.zhixun.enums.NotificationTypeEnum;
import com.zhixun.mapper.ArticleLikeMapper;
import com.zhixun.mapper.ArticleMapper;
import com.zhixun.mapper.CommentMapper;
import com.zhixun.mapper.CommentReportMapper;
import com.zhixun.mapper.UserMapper;
import com.zhixun.security.HtmlWhitelistFilter;
import com.zhixun.service.CommentService;
import com.zhixun.service.NotificationService;
import com.zhixun.config.RedisConfig;
import com.zhixun.vo.CommentReportVO;
import com.zhixun.vo.CommentVO;
import com.zhixun.vo.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
    private final ArticleLikeMapper articleLikeMapper;
    private final UserMapper userMapper;
    private final CommentReportMapper commentReportMapper;
    private final SensitiveWordUtil sensitiveWordUtil;
    private final NotificationService notificationService;
    private final SecurityUtil securityUtil;

    /** Redis 模板（可选，Redis 不可用时为 null） */
    @Autowired(required = false)
    private StringRedisTemplate stringRedisTemplate;

    /** @提及正则：匹配 @username（用户名由字母、数字、下划线、中文组成，2-20个字符） */
    private static final Pattern MENTION_PATTERN = Pattern.compile("@([\\w\\u4e00-\\u9fa5]{2,20})");

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CommentVO createComment(Long userId, CommentCreateRequest request) {
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
        comment.setContent(HtmlWhitelistFilter.escapePlainText(request.getContent()));
        comment.setStatus(CommentStatusEnum.PENDING);
        comment.setLikeCount(0);

        commentMapper.insert(comment);

        // 构建返回的 CommentVO（含用户信息和时间）
        User currentUser = userMapper.selectById(userId);
        Map<Long, User> userMap = new java.util.HashMap<>();
        if (currentUser != null) {
            userMap.put(userId, currentUser);
        }
        // 如果是回复，还需要查询被回复用户的信息
        if (request.getReplyUserId() != null && !request.getReplyUserId().equals(userId)) {
            User replyUser = userMapper.selectById(request.getReplyUserId());
            if (replyUser != null) {
                userMap.put(request.getReplyUserId(), replyUser);
            }
        }
        CommentVO commentVO = buildCommentVO(comment, userMap);

        // 评论默认待审核，审核通过后再增加文章评论数

        // 发送回复通知：如果是回复评论，通知被回复的评论作者（非关键操作，失败不影响评论创建）
        try {
            sendReplyNotification(userId, comment, request);
        } catch (Exception e) {
            log.error("发送评论回复通知失败, commentId={}: {}", comment.getId(), e.getMessage());
        }

        // 解析@提及并发送通知（非关键操作，失败不影响评论创建）
        try {
            sendMentionNotifications(userId, comment, request.getContent());
        } catch (Exception e) {
            log.error("发送评论@提及通知失败, commentId={}: {}", comment.getId(), e.getMessage());
        }

        // 递增数据版本号，通知客户端数据已变更
        try {
            if (stringRedisTemplate != null) {
                RedisConfig.incrementDataVersion(stringRedisTemplate);
            }
        } catch (Exception e) {
            log.debug("递增数据版本号失败: {}", e.getMessage());
        }

        return commentVO;
    }

    @Override
    @Slave
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
        if (!securityUtil.isOwnerOrAdmin(comment.getUserId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权删除此评论");
        }

        // 软删除：更新状态为已删除
        CommentStatusEnum originalStatus = comment.getStatus();
        comment.setStatus(CommentStatusEnum.DELETED);
        commentMapper.updateById(comment);

        // 减少文章评论数（仅当评论之前是正常状态时才减少，PENDING 状态的评论还未计入评论数）
        if (originalStatus == CommentStatusEnum.NORMAL) {
            articleMapper.update(null, new LambdaUpdateWrapper<Article>()
                    .eq(Article::getId, comment.getArticleId())
                    .gt(Article::getCommentCount, 0)
                    .setSql("comment_count = comment_count - 1"));
        }

        // 递增数据版本号，通知客户端数据已变更
        try {
            if (stringRedisTemplate != null) {
                RedisConfig.incrementDataVersion(stringRedisTemplate);
            }
        } catch (Exception e) {
            log.debug("递增数据版本号失败: {}", e.getMessage());
        }
    }

    @Override
    @Slave
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditComment(Long userId, Long commentId, String action, String reason) {
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "评论不存在");
        }

        if ("approve".equals(action)) {
            comment.setStatus(CommentStatusEnum.NORMAL);
            commentMapper.updateById(comment);

            // 审核通过，增加文章评论数（SQL 原子操作）
            articleMapper.update(null, new LambdaUpdateWrapper<Article>()
                    .eq(Article::getId, comment.getArticleId())
                    .setSql("comment_count = comment_count + 1"));
        } else if ("reject".equals(action)) {
            comment.setStatus(CommentStatusEnum.DELETED);
            commentMapper.updateById(comment);
        } else {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "无效的审核动作");
        }

        log.info("管理员{}审核评论{}，动作：{}，原因：{}", userId, commentId, action, reason);

        // 递增数据版本号，通知客户端数据已变更
        try {
            if (stringRedisTemplate != null) {
                RedisConfig.incrementDataVersion(stringRedisTemplate);
            }
        } catch (Exception e) {
            log.debug("递增数据版本号失败: {}", e.getMessage());
        }
    }

    @Override
    @Slave
    public PageResult<CommentVO> getPendingComments(Integer page, Integer pageSize) {
        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Comment::getStatus, CommentStatusEnum.PENDING)
                .orderByAsc(Comment::getCreatedAt);

        Page<Comment> commentPage = new Page<>(page, pageSize);
        Page<Comment> result = commentMapper.selectPage(commentPage, wrapper);

        List<CommentVO> voList = convertToVOList(result.getRecords());

        return new PageResult<>(voList, result.getTotal(), page, pageSize);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reportComment(Long userId, Long commentId, CommentReportRequest request) {
        // 校验评论是否存在
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "评论不存在");
        }

        // 检查是否重复举报
        LambdaQueryWrapper<CommentReport> existWrapper = new LambdaQueryWrapper<>();
        existWrapper.eq(CommentReport::getCommentId, commentId)
                .eq(CommentReport::getReporterId, userId);
        if (commentReportMapper.selectCount(existWrapper) > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "您已举报过该评论");
        }

        // 创建举报记录
        CommentReport report = new CommentReport();
        report.setCommentId(commentId);
        report.setReporterId(userId);
        report.setReason(request.getReason());
        report.setStatus(0);
        commentReportMapper.insert(report);

        log.info("用户{}举报评论{}，原因：{}", userId, commentId, request.getReason());
    }

    @Override
    @Slave
    public PageResult<CommentReportVO> getReports(Integer status, Integer page, Integer pageSize) {
        LambdaQueryWrapper<CommentReport> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(CommentReport::getStatus, status);
        }
        wrapper.orderByDesc(CommentReport::getCreatedAt);

        Page<CommentReport> reportPage = new Page<>(page, pageSize);
        Page<CommentReport> result = commentReportMapper.selectPage(reportPage, wrapper);

        List<CommentReportVO> voList = convertReportToVOList(result.getRecords());

        return new PageResult<>(voList, result.getTotal(), page, pageSize);
    }

    // ========== 内部方法 ==========

    /**
     * 发送回复通知：如果是回复评论，通知被回复的评论作者
     */
    private void sendReplyNotification(Long userId, Comment comment, CommentCreateRequest request) {
        // 回复父评论
        if (request.getParentId() != null && request.getParentId() > 0) {
            Comment parentComment = commentMapper.selectById(request.getParentId());
            if (parentComment != null && !parentComment.getUserId().equals(userId)) {
                User replier = userMapper.selectById(userId);
                String replierName = replier != null ? replier.getNickname() : "用户";
                String contentPreview = comment.getContent().length() > 50
                        ? comment.getContent().substring(0, 50) + "..."
                        : comment.getContent();
                notificationService.createNotification(
                        parentComment.getUserId(),
                        NotificationTypeEnum.COMMENT_REPLY.getValue(),
                        "收到评论回复",
                        replierName + " 回复了你的评论：" + contentPreview,
                        comment.getId()
                );
            }
        }

        // 回复指定用户（replyUserId）
        if (request.getReplyUserId() != null && !request.getReplyUserId().equals(userId)) {
            // 避免重复通知（如果 replyUserId 和父评论作者相同，上面已通知）
            if (request.getParentId() == null || request.getParentId() == 0) {
                User replier = userMapper.selectById(userId);
                String replierName = replier != null ? replier.getNickname() : "用户";
                String contentPreview = comment.getContent().length() > 50
                        ? comment.getContent().substring(0, 50) + "..."
                        : comment.getContent();
                notificationService.createNotification(
                        request.getReplyUserId(),
                        NotificationTypeEnum.COMMENT_REPLY.getValue(),
                        "收到评论回复",
                        replierName + " 回复了你：" + contentPreview,
                        comment.getId()
                );
            }
        }
    }

    /**
     * 解析@提及并发送通知
     */
    private void sendMentionNotifications(Long userId, Comment comment, String rawContent) {
        List<String> mentionUsernames = extractMentions(rawContent);
        if (CollectionUtils.isEmpty(mentionUsernames)) {
            return;
        }

        User replier = userMapper.selectById(userId);
        String replierName = replier != null ? replier.getNickname() : "用户";

        for (String username : mentionUsernames) {
            // 根据用户名查找用户
            LambdaQueryWrapper<User> userWrapper = new LambdaQueryWrapper<>();
            userWrapper.eq(User::getUsername, username);
            User mentionedUser = userMapper.selectOne(userWrapper);

            if (mentionedUser != null && !mentionedUser.getId().equals(userId)) {
                String contentPreview = comment.getContent().length() > 50
                        ? comment.getContent().substring(0, 50) + "..."
                        : comment.getContent();
                notificationService.createNotification(
                        mentionedUser.getId(),
                        NotificationTypeEnum.MENTION.getValue(),
                        "有人在评论中提及了你",
                        replierName + " 在评论中提及了你：" + contentPreview,
                        comment.getId()
                );
            }
        }
    }

    /**
     * 从文本中提取@提及的用户名列表
     *
     * @param text 文本内容
     * @return 提及的用户名列表（去重）
     */
    private List<String> extractMentions(String text) {
        if (!StringUtils.hasText(text)) {
            return Collections.emptyList();
        }

        List<String> mentions = new ArrayList<>();
        Matcher matcher = MENTION_PATTERN.matcher(text);
        while (matcher.find()) {
            mentions.add(matcher.group(1));
        }
        return mentions.stream().distinct().collect(Collectors.toList());
    }

    /**
     * 将举报列表转换为 VO 列表
     */
    private List<CommentReportVO> convertReportToVOList(List<CommentReport> reports) {
        if (CollectionUtils.isEmpty(reports)) {
            return Collections.emptyList();
        }

        // 批量查询关联的评论
        List<Long> commentIds = reports.stream()
                .map(CommentReport::getCommentId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, Comment> commentMap = commentMapper.selectBatchIds(commentIds).stream()
                .collect(Collectors.toMap(Comment::getId, c -> c));

        // 批量查询举报人信息
        List<Long> reporterIds = reports.stream()
                .map(CommentReport::getReporterId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, User> userMap = userMapper.selectBatchIds(reporterIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        return reports.stream().map(report -> {
            CommentReportVO vo = new CommentReportVO();
            vo.setId(report.getId());
            vo.setCommentId(report.getCommentId());
            vo.setReporterId(report.getReporterId());
            vo.setReason(report.getReason());
            vo.setStatus(report.getStatus());
            vo.setCreatedAt(report.getCreatedAt());

            // 评论内容摘要
            Comment comment = commentMap.get(report.getCommentId());
            if (comment != null) {
                String content = comment.getContent();
                vo.setCommentContent(content != null && content.length() > 50
                        ? content.substring(0, 50) + "..."
                        : content);
            }

            // 举报人昵称
            User reporter = userMap.get(report.getReporterId());
            if (reporter != null) {
                vo.setReporterNickname(reporter.getNickname());
            }

            return vo;
        }).collect(Collectors.toList());
    }

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

        // 批量查询当前用户点赞状态
        Long currentUserId = null;
        try {
            currentUserId = securityUtil.getCurrentUserId();
        } catch (Exception ignored) {}

        final java.util.Set<Long> likedCommentIds;
        if (currentUserId != null) {
            List<Long> allCommentIds = allComments.stream().map(Comment::getId).collect(Collectors.toList());
            java.util.Set<Long> liked = java.util.Collections.emptySet();
            try {
                List<ArticleLike> likes = articleLikeMapper.selectList(
                        new LambdaQueryWrapper<ArticleLike>()
                                .eq(ArticleLike::getUserId, currentUserId)
                                .eq(ArticleLike::getTargetType, LikeTargetTypeEnum.COMMENT)
                                .in(ArticleLike::getTargetId, allCommentIds));
                liked = likes.stream().map(ArticleLike::getTargetId).collect(Collectors.toSet());
            } catch (Exception e) {
                log.warn("批量查询评论点赞状态失败: {}", e.getMessage());
            }
            likedCommentIds = liked;
        } else {
            likedCommentIds = java.util.Collections.emptySet();
        }

        // 按父评论ID分组子评论
        Map<Long, List<Comment>> childrenMap = allChildren.stream()
                .collect(Collectors.groupingBy(Comment::getParentId));

        // 转换顶级评论
        return comments.stream().map(comment -> {
            CommentVO vo = buildCommentVO(comment, userMap);
            vo.setIsLiked(likedCommentIds.contains(comment.getId()));

            // 设置子评论
            List<Comment> children = childrenMap.getOrDefault(comment.getId(), Collections.emptyList());
            List<CommentVO> childVOs = children.stream()
                    .map(child -> {
                        CommentVO childVo = buildCommentVO(child, userMap);
                        childVo.setIsLiked(likedCommentIds.contains(child.getId()));
                        return childVo;
                    })
                    .collect(Collectors.toList());
            vo.setReplies(childVOs);

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
        vo.setUserId(comment.getUserId());
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
        vo.setFollowCount(user.getFollowCount());
        vo.setFollowerCount(user.getFollowerCount());
        vo.setArticleCount(user.getArticleCount());
        vo.setBio(user.getBio());
        vo.setProvince(user.getProvince());
        vo.setIpLocation(user.getIpLocation());
        return vo;
    }
}
