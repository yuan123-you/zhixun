package com.zhixun.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhixun.common.exception.BusinessException;
import com.zhixun.common.result.ErrorCode;
import com.zhixun.common.result.PageResult;
import com.zhixun.dto.admin.AuditRequest;
import com.zhixun.dto.admin.SensitiveWhitelistRequest;
import com.zhixun.dto.admin.SensitiveWordRequest;
import com.zhixun.dto.admin.UserStatusRequest;
import com.zhixun.entity.Article;
import com.zhixun.entity.ArticleCollaborator;
import com.zhixun.entity.ArticleLike;
import com.zhixun.entity.ArticleTag;
import com.zhixun.entity.ArticleViewHistory;
import com.zhixun.entity.Category;
import com.zhixun.entity.Comment;
import com.zhixun.entity.GroupInfo;
import com.zhixun.entity.GroupMember;
import com.zhixun.entity.GroupMessage;
import com.zhixun.entity.LoginLog;
import com.zhixun.entity.Notification;
import com.zhixun.entity.OperationLog;
import com.zhixun.entity.SecurityAuditLog;
import com.zhixun.entity.SensitiveWhitelist;
import com.zhixun.entity.SensitiveWord;
import com.zhixun.entity.Tag;
import com.zhixun.entity.User;
import com.zhixun.entity.UserMessage;
import com.zhixun.enums.ArticleStatusEnum;
import com.zhixun.enums.CommentStatusEnum;
import com.zhixun.enums.LikeTargetTypeEnum;
import com.zhixun.enums.NotificationTypeEnum;
import com.zhixun.enums.RoleEnum;
import com.zhixun.enums.SensitiveLevelEnum;
import com.zhixun.mapper.ArticleLikeMapper;
import com.zhixun.mapper.ArticleMapper;
import com.zhixun.mapper.ArticleTagMapper;
import com.zhixun.mapper.ArticleViewHistoryMapper;
import com.zhixun.mapper.ArticleCollaboratorMapper;
import com.zhixun.mapper.CategoryMapper;
import com.zhixun.mapper.CommentMapper;
import com.zhixun.mapper.GroupMapper;
import com.zhixun.mapper.GroupMemberMapper;
import com.zhixun.mapper.GroupMessageMapper;
import com.zhixun.mapper.LoginLogMapper;
import com.zhixun.mapper.NotificationMapper;
import com.zhixun.mapper.OperationLogMapper;
import com.zhixun.mapper.SecurityAuditLogMapper;
import com.zhixun.mapper.SensitiveWhitelistMapper;
import com.zhixun.mapper.SensitiveWordMapper;
import com.zhixun.mapper.TagMapper;
import com.zhixun.mapper.UserMapper;
import com.zhixun.mapper.UserMessageMapper;
import com.zhixun.mapper.ViewHistoryMapper;
import com.zhixun.common.util.SensitiveWordUtil;
import com.zhixun.service.AdminService;
import com.zhixun.service.NotificationService;
import com.zhixun.service.OperationLogService;
import com.zhixun.vo.ArticleVO;
import com.zhixun.vo.CollaboratorVO;
import com.zhixun.vo.CommentVO;
import com.zhixun.vo.ConversationVO;
import com.zhixun.vo.DashboardVO;
import com.zhixun.vo.GroupMessageVO;
import com.zhixun.vo.GroupVO;
import com.zhixun.vo.MessageVO;
import com.zhixun.vo.NotificationVO;
import com.zhixun.vo.TagVO;
import com.zhixun.vo.UserVO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 后台管理服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final ArticleMapper articleMapper;
    private final ArticleLikeMapper articleLikeMapper;
    private final ArticleTagMapper articleTagMapper;
    private final ArticleViewHistoryMapper articleViewHistoryMapper;
    private final UserMapper userMapper;
    private final CategoryMapper categoryMapper;
    private final TagMapper tagMapper;
    private final CommentMapper commentMapper;
    private final SensitiveWordMapper sensitiveWordMapper;
    private final SensitiveWhitelistMapper sensitiveWhitelistMapper;
    private final OperationLogMapper operationLogMapper;
    private final SecurityAuditLogMapper securityAuditLogMapper;
    private final NotificationMapper notificationMapper;
    private final LoginLogMapper loginLogMapper;
    private final ViewHistoryMapper viewHistoryMapper;
    private final GroupMapper groupMapper;
    private final GroupMemberMapper groupMemberMapper;
    private final GroupMessageMapper groupMessageMapper;
    private final ArticleCollaboratorMapper articleCollaboratorMapper;
    private final UserMessageMapper userMessageMapper;
    private final OperationLogService operationLogService;
    private final SensitiveWordUtil sensitiveWordUtil;
    private final NotificationService notificationService;

    @Override
    public PageResult<ArticleVO> getPendingArticles(Integer page, Integer pageSize) {
        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Article::getStatus, ArticleStatusEnum.PENDING)
                .orderByDesc(Article::getCreatedAt);

        Page<Article> articlePage = new Page<>(page, pageSize);
        Page<Article> result = articleMapper.selectPage(articlePage, wrapper);

        List<ArticleVO> voList = convertToVOList(result.getRecords());
        return new PageResult<>(voList, result.getTotal(), page, pageSize);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditArticle(Long adminId, Long articleId, AuditRequest request) {
        Article article = articleMapper.selectById(articleId);
        if (article == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "作品不存在");
        }

        // 审核状态只能从待审核变更
        if (article.getStatus() != ArticleStatusEnum.PENDING) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "作品当前状态不允许审核");
        }

        String action;
        String detail;

        if (request.getStatus() == Article.STATUS_PUBLISHED) {
            // 审核通过
            article.setStatus(ArticleStatusEnum.PUBLISHED);
            article.setPublishAt(LocalDateTime.now());
            article.setRejectReason(null);
            action = "approve";
            detail = "审核通过";
        } else if (request.getStatus() == Article.STATUS_REJECTED) {
            // 审核驳回
            article.setStatus(ArticleStatusEnum.REJECTED);
            article.setRejectReason(request.getReason());
            action = "reject";
            detail = "审核驳回，原因：" + request.getReason();
        } else {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持的操作类型");
        }

        articleMapper.updateById(article);

        // 记录操作日志（异步）
        String ip = getClientIp();
        operationLogService.log(adminId, "作品审核", action, "article", articleId, detail, ip);

        // 发送通知给作者
        sendAuditNotification(article.getAuthorId(), articleId, action, request.getReason());
    }

    @Override
    public DashboardVO getDashboardOverview() {
        return getDashboardOverview("daily");
    }

    /**
     * 获取数据概览（支持时间维度）
     *
     * @param period 时间周期：daily/weekly/monthly
     * @return 概览数据
     */
    public DashboardVO getDashboardOverview(String period) {
        DashboardVO vo = new DashboardVO();
        // 预置默认值，防止 try 块异常时 trend 为 null 导致前端崩溃
        DashboardVO.TrendData defaultTrend = new DashboardVO.TrendData();
        defaultTrend.setDates(Collections.emptyList());
        defaultTrend.setViews(Collections.emptyList());
        defaultTrend.setUsers(Collections.emptyList());
        vo.setTrend(defaultTrend);
        try {
            LocalDateTime todayStart = LocalDate.now().atStartOfDay();

            // 用户总量
            vo.setUserTotal(userMapper.selectCount(
                    new LambdaQueryWrapper<>()));

            // 作品总量
            vo.setArticleTotal(articleMapper.selectCount(
                    new LambdaQueryWrapper<Article>().eq(Article::getStatus, ArticleStatusEnum.PUBLISHED)));

            // 今日日活用户数
            vo.setTodayDau(userMapper.selectCount(
                    new LambdaQueryWrapper<User>()
                            .ge(User::getLastLoginAt, todayStart)));

            // 今日浏览量
            vo.setTodayView(articleViewHistoryMapper.selectCount(
                    new LambdaQueryWrapper<ArticleViewHistory>()
                            .ge(ArticleViewHistory::getCreateTime, todayStart)));

            // 今日点赞数
            vo.setTodayLike(articleLikeMapper.selectCount(
                    new LambdaQueryWrapper<ArticleLike>()
                            .eq(ArticleLike::getTargetType, LikeTargetTypeEnum.ARTICLE)
                            .ge(ArticleLike::getCreatedAt, todayStart)));

            // 今日评论数
            vo.setTodayComment(commentMapper.selectCount(
                    new LambdaQueryWrapper<Comment>()
                            .ge(Comment::getCreatedAt, todayStart)));

            // 近7天趋势数据
            DashboardVO.TrendData trendData = new DashboardVO.TrendData();
            List<String> dates = new ArrayList<>();
            List<Long> views = new ArrayList<>();
            List<Long> users = new ArrayList<>();

            for (int i = 6; i >= 0; i--) {
                LocalDate date = LocalDate.now().minusDays(i);
                LocalDateTime dayStart = date.atStartOfDay();
                LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();

                dates.add(date.format(DateTimeFormatter.ofPattern("MM-dd")));
                views.add(articleViewHistoryMapper.selectCount(
                        new LambdaQueryWrapper<ArticleViewHistory>()
                                .ge(ArticleViewHistory::getCreateTime, dayStart)
                                .lt(ArticleViewHistory::getCreateTime, dayEnd)));
                users.add(userMapper.selectCount(
                        new LambdaQueryWrapper<User>()
                                .ge(User::getCreatedAt, dayStart)
                                .lt(User::getCreatedAt, dayEnd)));
            }

            trendData.setDates(dates);
            trendData.setViews(views);
            trendData.setUsers(users);
            vo.setTrend(trendData);
        } catch (Exception e) {
            log.error("获取 Dashboard 基础数据失败", e);
        }

        // 子模块独立容错，避免单个失败导致整个接口 500
        try {
            vo.setRetentionRates(getRetentionRates());
        } catch (Exception e) {
            log.error("计算留存率失败", e);
            vo.setRetentionRates(Collections.emptyList());
        }

        try {
            vo.setActivityDistributions(getActivityDistributions());
        } catch (Exception e) {
            log.error("计算活跃度分布失败", e);
            vo.setActivityDistributions(Collections.emptyList());
        }

        try {
            vo.setGrowthTrends(getGrowthTrends(period));
        } catch (Exception e) {
            log.error("计算增长趋势失败", e);
            vo.setGrowthTrends(Collections.emptyList());
        }

        try {
            vo.setCategoryDistributions(getCategoryDistributions());
        } catch (Exception e) {
            log.error("计算分类分布失败", e);
            vo.setCategoryDistributions(Collections.emptyList());
        }

        try {
            vo.setHotArticleRanks(getHotArticleRanks());
        } catch (Exception e) {
            log.error("计算热门作品失败", e);
            vo.setHotArticleRanks(Collections.emptyList());
        }

        try {
            vo.setCreatorRanks(getCreatorRanks());
        } catch (Exception e) {
            log.error("计算创作者排行失败", e);
            vo.setCreatorRanks(Collections.emptyList());
        }

        return vo;
    }

    @Override
    public PageResult<UserVO> getUserList(String keyword, String role, Integer status, Integer page, Integer pageSize) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();

        // 关键词搜索
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(User::getUsername, keyword)
                    .or().like(User::getNickname, keyword)
                    .or().like(User::getEmail, keyword)
                    .or().like(User::getUid, keyword));
        }

        // 角色筛选（兼容大小写）
        if (StringUtils.hasText(role)) {
            wrapper.eq(User::getRole, RoleEnum.valueOf(role.toUpperCase()));
        }

        // 状态筛选
        if (status != null) {
            wrapper.eq(User::getStatus, status);
        }

        wrapper.orderByDesc(User::getCreatedAt);

        Page<User> userPage = new Page<>(page, pageSize);
        Page<User> result = userMapper.selectPage(userPage, wrapper);

        List<UserVO> voList = result.getRecords().stream().map(user -> {
            UserVO vo = new UserVO();
            vo.setId(user.getId());
            vo.setUid(user.getUid());
            vo.setUsername(user.getUsername());
            vo.setNickname(user.getNickname());
            vo.setAvatar(user.getAvatar());
            vo.setEmail(user.getEmail());
            vo.setPhone(user.getPhone());
            vo.setRole(user.getRole() != null ? user.getRole().getValue() : null);
            vo.setStatus(user.getStatus());
            vo.setCreatedAt(user.getCreatedAt());
            vo.setFollowCount(user.getFollowCount());
            vo.setFollowerCount(user.getFollowerCount());
            vo.setArticleCount(user.getArticleCount());
            return vo;
        }).collect(Collectors.toList());

        return new PageResult<>(voList, result.getTotal(), page, pageSize);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUserStatus(Long adminId, Long userId, UserStatusRequest request) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        user.setStatus(request.getStatus());
        userMapper.updateById(user);

        // 记录操作日志（异步）
        String action = request.getStatus() == User.STATUS_DISABLED ? "ban" : "unban";
        String detail = request.getStatus() == User.STATUS_DISABLED
                ? "封禁用户，原因：" + request.getReason()
                : "解封用户";
        String ip = getClientIp();
        operationLogService.log(adminId, "用户管理", action, "user", userId, detail, ip);
    }

    @Override
    public PageResult<SensitiveWord> getSensitiveWords(Integer page, Integer pageSize) {
        LambdaQueryWrapper<SensitiveWord> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(SensitiveWord::getCreatedAt);

        Page<SensitiveWord> wordPage = new Page<>(page, pageSize);
        Page<SensitiveWord> result = sensitiveWordMapper.selectPage(wordPage, wrapper);

        return new PageResult<>(result.getRecords(), result.getTotal(), page, pageSize);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addSensitiveWord(Long adminId, SensitiveWordRequest request) {
        // 检查是否已存在
        Long count = sensitiveWordMapper.selectCount(
                new LambdaQueryWrapper<SensitiveWord>().eq(SensitiveWord::getWord, request.getWord()));
        if (count > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "敏感词已存在");
        }

        SensitiveWord word = new SensitiveWord();
        word.setWord(request.getWord());
        word.setLevel(SensitiveLevelEnum.values()[request.getLevel() - 1]);
        sensitiveWordMapper.insert(word);

        // 清除敏感词本地缓存并重建 DFA
        sensitiveWordUtil.evictCache();
        sensitiveWordUtil.buildDFA();

        // 记录操作日志
        String ip = getClientIp();
        operationLogService.log(adminId, "敏感词管理", "add", "sensitive_word", word.getId(),
                "添加敏感词：" + request.getWord(), ip);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSensitiveWord(Long adminId, Long wordId) {
        SensitiveWord word = sensitiveWordMapper.selectById(wordId);
        if (word == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "敏感词不存在");
        }

        sensitiveWordMapper.deleteById(wordId);

        // 清除敏感词本地缓存并重建 DFA
        sensitiveWordUtil.evictCache();
        sensitiveWordUtil.buildDFA();

        // 记录操作日志
        String ip = getClientIp();
        operationLogService.log(adminId, "敏感词管理", "delete", "sensitive_word", wordId,
                "删除敏感词：" + word.getWord(), ip);
    }

    @Override
    public PageResult<CommentVO> getCommentList(Long articleId, Integer status, Integer page, Integer pageSize) {
        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<>();

        // 作品ID筛选
        if (articleId != null) {
            wrapper.eq(Comment::getArticleId, articleId);
        }

        // 状态筛选
        if (status != null) {
            wrapper.eq(Comment::getStatus, CommentStatusEnum.values()[status]);
        }

        // 不查询已软删除的评论
        wrapper.isNull(Comment::getDeletedAt);
        wrapper.orderByDesc(Comment::getCreatedAt);

        Page<Comment> commentPage = new Page<>(page, pageSize);
        Page<Comment> result = commentMapper.selectPage(commentPage, wrapper);

        // 收集用户ID
        Set<Long> userIds = result.getRecords().stream()
                .map(Comment::getUserId)
                .collect(Collectors.toSet());
        result.getRecords().stream()
                .map(Comment::getReplyToId)
                .filter(id -> id != null)
                .forEach(userIds::add);

        Map<Long, User> userMap = userIds.isEmpty() ? Collections.emptyMap()
                : userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        List<CommentVO> voList = result.getRecords().stream().map(comment -> {
            CommentVO vo = new CommentVO();
            vo.setId(comment.getId());
            vo.setArticleId(comment.getArticleId());
            vo.setContent(comment.getContent());
            vo.setStatus(comment.getStatus() != null ? comment.getStatus().getValue() : null);
            vo.setLikeCount(comment.getLikeCount());
            vo.setParentId(comment.getParentId());
            vo.setCreatedAt(comment.getCreatedAt());

            User user = userMap.get(comment.getUserId());
            if (user != null) {
                UserVO userVO = new UserVO();
                userVO.setId(user.getId());
                userVO.setUsername(user.getUsername());
                userVO.setNickname(user.getNickname());
                userVO.setAvatar(user.getAvatar());
                vo.setUser(userVO);
            }

            if (comment.getReplyToId() != null) {
                User replyUser = userMap.get(comment.getReplyToId());
                if (replyUser != null) {
                    UserVO replyUserVO = new UserVO();
                    replyUserVO.setId(replyUser.getId());
                    replyUserVO.setUsername(replyUser.getUsername());
                    replyUserVO.setNickname(replyUser.getNickname());
                    replyUserVO.setAvatar(replyUser.getAvatar());
                    vo.setReplyUser(replyUserVO);
                }
            }

            return vo;
        }).collect(Collectors.toList());

        return new PageResult<>(voList, result.getTotal(), page, pageSize);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCommentAsAdmin(Long adminId, Long commentId) {
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "评论不存在");
        }

        // 减少作品评论数（仅当评论之前是正常状态时才减少）
        CommentStatusEnum originalStatus = comment.getStatus();
        if (originalStatus == CommentStatusEnum.NORMAL) {
            articleMapper.update(null, new LambdaUpdateWrapper<Article>()
                    .eq(Article::getId, comment.getArticleId())
                    .gt(Article::getCommentCount, 0)
                    .setSql("comment_count = comment_count - 1"));
        }

        // 软删除
        comment.setStatus(CommentStatusEnum.DELETED);
        comment.setDeletedAt(LocalDateTime.now());
        commentMapper.updateById(comment);

        // 记录操作日志
        String ip = getClientIp();
        operationLogService.log(adminId, "评论管理", "delete", "comment", commentId,
                "删除评论：" + comment.getContent(), ip);
    }

    @Override
    public PageResult<SensitiveWhitelist> getSensitiveWhitelist(Integer page, Integer pageSize) {
        LambdaQueryWrapper<SensitiveWhitelist> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(SensitiveWhitelist::getCreatedAt);

        Page<SensitiveWhitelist> whitelistPage = new Page<>(page, pageSize);
        Page<SensitiveWhitelist> result = sensitiveWhitelistMapper.selectPage(whitelistPage, wrapper);

        return new PageResult<>(result.getRecords(), result.getTotal(), page, pageSize);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addSensitiveWhitelist(Long adminId, SensitiveWhitelistRequest request) {
        // 检查是否已存在
        Long count = sensitiveWhitelistMapper.selectCount(
                new LambdaQueryWrapper<SensitiveWhitelist>().eq(SensitiveWhitelist::getWord, request.getWord()));
        if (count > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "白名单词汇已存在");
        }

        SensitiveWhitelist whitelist = new SensitiveWhitelist();
        whitelist.setWord(request.getWord());
        whitelist.setCreatedBy(adminId);
        sensitiveWhitelistMapper.insert(whitelist);

        // 清除白名单缓存
        sensitiveWordUtil.evictWhitelistCache();

        // 记录操作日志
        String ip = getClientIp();
        operationLogService.log(adminId, "敏感词白名单", "add", "sensitive_whitelist", whitelist.getId(),
                "添加白名单词汇：" + request.getWord(), ip);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSensitiveWhitelist(Long adminId, Long id) {
        SensitiveWhitelist whitelist = sensitiveWhitelistMapper.selectById(id);
        if (whitelist == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "白名单词汇不存在");
        }

        sensitiveWhitelistMapper.deleteById(id);

        // 清除白名单缓存
        sensitiveWordUtil.evictWhitelistCache();

        // 记录操作日志
        String ip = getClientIp();
        operationLogService.log(adminId, "敏感词白名单", "delete", "sensitive_whitelist", id,
                "删除白名单词汇：" + whitelist.getWord(), ip);
    }

    @Override
    public PageResult<SecurityAuditLog> getSecurityAuditLogs(String eventType, Long userId, String ip,
                                                              String startDate, String endDate,
                                                              Integer page, Integer pageSize) {
        LambdaQueryWrapper<SecurityAuditLog> wrapper = new LambdaQueryWrapper<>();

        // 事件类型筛选
        if (StringUtils.hasText(eventType)) {
            wrapper.eq(SecurityAuditLog::getEventType, eventType);
        }

        // 用户ID筛选
        if (userId != null) {
            wrapper.eq(SecurityAuditLog::getUserId, userId);
        }

        // IP筛选
        if (StringUtils.hasText(ip)) {
            wrapper.like(SecurityAuditLog::getIp, ip);
        }

        // 日期范围筛选
        if (StringUtils.hasText(startDate)) {
            LocalDateTime start = LocalDate.parse(startDate).atStartOfDay();
            wrapper.ge(SecurityAuditLog::getCreatedAt, start);
        }
        if (StringUtils.hasText(endDate)) {
            LocalDateTime end = LocalDate.parse(endDate).plusDays(1).atStartOfDay();
            wrapper.lt(SecurityAuditLog::getCreatedAt, end);
        }

        wrapper.orderByDesc(SecurityAuditLog::getCreatedAt);

        Page<SecurityAuditLog> auditPage = new Page<>(page, pageSize);
        Page<SecurityAuditLog> result = securityAuditLogMapper.selectPage(auditPage, wrapper);

        return new PageResult<>(result.getRecords(), result.getTotal(), page, pageSize);
    }

    @Override
    public Map<String, Object> getSecurityAuditStats(String startDate, String endDate) {
        Map<String, Object> stats = new java.util.HashMap<>();

        LambdaQueryWrapper<SecurityAuditLog> wrapper = new LambdaQueryWrapper<>();

        // 日期范围筛选
        if (StringUtils.hasText(startDate)) {
            LocalDateTime start = LocalDate.parse(startDate).atStartOfDay();
            wrapper.ge(SecurityAuditLog::getCreatedAt, start);
        }
        if (StringUtils.hasText(endDate)) {
            LocalDateTime end = LocalDate.parse(endDate).plusDays(1).atStartOfDay();
            wrapper.lt(SecurityAuditLog::getCreatedAt, end);
        }

        // 总事件数
        long total = securityAuditLogMapper.selectCount(wrapper);
        stats.put("total", total);

        // 按事件类型统计
        List<Map<String, Object>> eventTypeStats = new ArrayList<>();
        List<SecurityAuditLog> allLogs = securityAuditLogMapper.selectList(wrapper);
        Map<String, Long> eventTypeCount = allLogs.stream()
                .collect(Collectors.groupingBy(SecurityAuditLog::getEventType, Collectors.counting()));
        eventTypeCount.forEach((type, count) -> {
            Map<String, Object> item = new java.util.HashMap<>();
            item.put("eventType", type);
            item.put("count", count);
            eventTypeStats.add(item);
        });
        stats.put("eventTypeStats", eventTypeStats);

        // 按日期统计（近7天或指定范围内）
        List<Map<String, Object>> dailyStats = new ArrayList<>();
        LocalDate statsStart;
        LocalDate statsEnd;
        if (StringUtils.hasText(startDate) && StringUtils.hasText(endDate)) {
            statsStart = LocalDate.parse(startDate);
            statsEnd = LocalDate.parse(endDate);
        } else {
            statsEnd = LocalDate.now();
            statsStart = statsEnd.minusDays(6);
        }

        for (LocalDate date = statsStart; !date.isAfter(statsEnd); date = date.plusDays(1)) {
            LocalDateTime dayStart = date.atStartOfDay();
            LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();

            LambdaQueryWrapper<SecurityAuditLog> dayWrapper = new LambdaQueryWrapper<>();
            dayWrapper.ge(SecurityAuditLog::getCreatedAt, dayStart);
            dayWrapper.lt(SecurityAuditLog::getCreatedAt, dayEnd);
            long dayCount = securityAuditLogMapper.selectCount(dayWrapper);

            Map<String, Object> dayItem = new java.util.HashMap<>();
            dayItem.put("date", date.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            dayItem.put("count", dayCount);
            dailyStats.add(dayItem);
        }
        stats.put("dailyStats", dailyStats);

        // 失败次数最多的IP（Top 10）
        List<Map<String, Object>> topIps = new ArrayList<>();
        Map<String, Long> ipCount = allLogs.stream()
                .filter(log -> log.getIp() != null)
                .collect(Collectors.groupingBy(SecurityAuditLog::getIp, Collectors.counting()));
        ipCount.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .forEach(entry -> {
                    Map<String, Object> item = new java.util.HashMap<>();
                    item.put("ip", entry.getKey());
                    item.put("count", entry.getValue());
                    topIps.add(item);
                });
        stats.put("topIps", topIps);

        return stats;
    }

    // ========== 内部方法 ==========

    /**
     * 发送审核通知给作者
     */
    private void sendAuditNotification(Long authorId, Long articleId, String action, String reason) {
        try {
            String title;
            String content;
            if ("approve".equals(action)) {
                title = "作品审核通过";
                content = "您的作品已通过审核，现已发布";
            } else {
                title = "作品审核驳回";
                content = "您的作品未通过审核，原因：" + (reason != null ? reason : "无");
            }
            notificationService.createNotification(
                    authorId,
                    NotificationTypeEnum.AUDIT.getValue(),
                    title,
                    content,
                    articleId
            );
        } catch (Exception e) {
            log.error("发送审核通知失败: {}", e.getMessage());
        }
    }

    /**
     * 获取客户端IP
     */
    private String getClientIp() {
        try {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String ip = request.getHeader("X-Forwarded-For");
                if (!StringUtils.hasText(ip) || "unknown".equalsIgnoreCase(ip)) {
                    ip = request.getHeader("X-Real-IP");
                }
                if (!StringUtils.hasText(ip) || "unknown".equalsIgnoreCase(ip)) {
                    ip = request.getRemoteAddr();
                }
                return ip;
            }
        } catch (Exception e) {
            // 忽略
        }
        return "unknown";
    }

    /**
     * 批量将作品实体列表转换为 VO 列表
     */
    private List<ArticleVO> convertToVOList(List<Article> articles) {
        if (CollectionUtils.isEmpty(articles)) {
            return Collections.emptyList();
        }

        Set<Long> userIds = articles.stream().map(Article::getAuthorId).collect(Collectors.toSet());
        Set<Long> categoryIds = articles.stream()
                .map(Article::getCategoryId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        List<Long> articleIds = articles.stream().map(Article::getId).collect(Collectors.toList());

        Map<Long, User> userMap = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        Map<Long, Category> categoryMap = CollectionUtils.isEmpty(categoryIds) ? Collections.emptyMap()
                : categoryMapper.selectBatchIds(categoryIds).stream()
                .collect(Collectors.toMap(Category::getId, c -> c));

        List<ArticleTag> allArticleTags = articleTagMapper.selectList(
                new LambdaQueryWrapper<ArticleTag>().in(ArticleTag::getArticleId, articleIds));
        Set<Long> tagIds = allArticleTags.stream().map(ArticleTag::getTagId).collect(Collectors.toSet());

        Map<Long, Tag> tagMap = CollectionUtils.isEmpty(tagIds) ? Collections.emptyMap()
                : tagMapper.selectBatchIds(tagIds).stream()
                .collect(Collectors.toMap(Tag::getId, t -> t));

        Map<Long, List<ArticleTag>> articleTagMap = allArticleTags.stream()
                .collect(Collectors.groupingBy(ArticleTag::getArticleId));

        return articles.stream().map(article -> {
            ArticleVO vo = new ArticleVO();
            vo.setId(article.getId());
            vo.setTitle(article.getTitle());
            vo.setSummary(article.getSummary());
            vo.setCoverImage(article.getCoverImage());
            vo.setStatus(article.getStatus() != null ? article.getStatus().getValue() : null);
            vo.setViewCount(article.getViewCount());
            vo.setLikeCount(article.getLikeCount());
            vo.setCommentCount(article.getCommentCount());
            vo.setCollectCount(article.getCollectCount());
            vo.setIsTop(article.getIsTop());
            vo.setCreatedAt(article.getCreatedAt());

            User author = userMap.get(article.getAuthorId());
            if (author != null) {
                vo.setAuthorName(author.getNickname());
                vo.setAuthorAvatar(author.getAvatar());
            }

            if (article.getCategoryId() != null) {
                Category category = categoryMap.get(article.getCategoryId());
                if (category != null) {
                    vo.setCategoryName(category.getName());
                }
            }

            List<ArticleTag> articleTags = articleTagMap.get(article.getId());
            if (!CollectionUtils.isEmpty(articleTags)) {
                List<TagVO> tags = articleTags.stream().map(at -> {
                    Tag tag = tagMap.get(at.getTagId());
                    if (tag != null) {
                        TagVO tagVO = new TagVO();
                        tagVO.setId(tag.getId());
                        tagVO.setName(tag.getName());
                        tagVO.setArticleCount(tag.getArticleCount());
                        tagVO.setCreatedAt(tag.getCreatedAt());
                        return tagVO;
                    }
                    return null;
                }).filter(t -> t != null).collect(Collectors.toList());
                vo.setTags(tags);
            } else {
                vo.setTags(Collections.emptyList());
            }

            return vo;
        }).collect(Collectors.toList());
    }

    // ========== 数据报表查询方法 ==========

    /**
     * 查询注册用户在后续7天内的登录留存率
     * 取近7天内每天注册的用户，统计其在注册后第N天是否登录
     */
    private List<DashboardVO.RetentionRate> getRetentionRates() {
        List<DashboardVO.RetentionRate> rates = new ArrayList<>();
        LocalDate today = LocalDate.now();

        // 取7天前注册的用户作为基准
        LocalDate cohortStart = today.minusDays(7);
        LocalDate cohortEnd = today.minusDays(6);
        LocalDateTime cohortStartDt = cohortStart.atStartOfDay();
        LocalDateTime cohortEndDt = cohortEnd.atStartOfDay();

        // 查询该批注册用户
        List<User> cohortUsers = userMapper.selectList(
                new LambdaQueryWrapper<User>()
                        .ge(User::getCreatedAt, cohortStartDt)
                        .lt(User::getCreatedAt, cohortEndDt));

        long cohortSize = cohortUsers.size();
        if (cohortSize == 0) {
            for (int day = 1; day <= 7; day++) {
                DashboardVO.RetentionRate rate = new DashboardVO.RetentionRate();
                rate.setDay(day);
                rate.setRate(BigDecimal.ZERO);
                rates.add(rate);
            }
            return rates;
        }

        for (int day = 1; day <= 7; day++) {
            LocalDateTime targetDayStart = cohortStart.plusDays(day).atStartOfDay();
            LocalDateTime targetDayEnd = cohortStart.plusDays(day + 1).atStartOfDay();

            // 统计该批用户在第N天有登录记录的人数
            long retained = 0;
            if (!cohortUsers.isEmpty()) {
                List<Long> cohortUserIds = cohortUsers.stream().map(User::getId).collect(Collectors.toList());
                retained = loginLogMapper.selectCount(
                        new LambdaQueryWrapper<LoginLog>()
                                .in(LoginLog::getUserId, cohortUserIds)
                                .ge(LoginLog::getCreatedAt, targetDayStart)
                                .lt(LoginLog::getCreatedAt, targetDayEnd));
            }

            DashboardVO.RetentionRate rate = new DashboardVO.RetentionRate();
            rate.setDay(day);
            rate.setRate(BigDecimal.valueOf(retained * 100.0 / cohortSize)
                    .setScale(2, RoundingMode.HALF_UP));
            rates.add(rate);
        }

        return rates;
    }

    /**
     * 按用户活跃度（高/中/低）分组统计
     * 高活跃：近7天登录>=5次；中活跃：1-4次；低活跃：0次
     */
    private List<DashboardVO.ActivityDistribution> getActivityDistributions() {
        List<DashboardVO.ActivityDistribution> distributions = new ArrayList<>();
        LocalDateTime weekAgo = LocalDateTime.now().minusWeeks(1);

        long totalUsers = userMapper.selectCount(new LambdaQueryWrapper<>());

        // 查询近7天有登录记录的用户及其登录次数
        List<LoginLog> recentLogs = loginLogMapper.selectList(
                new LambdaQueryWrapper<LoginLog>()
                        .ge(LoginLog::getCreatedAt, weekAgo)
                        .eq(LoginLog::getStatus, 1));

        // 按用户分组统计登录次数（过滤 null userId，避免 NPE）
        Map<Long, Long> userLoginCount = recentLogs.stream()
                .filter(log -> log.getUserId() != null)
                .collect(Collectors.groupingBy(LoginLog::getUserId, Collectors.counting()));

        long highCount = userLoginCount.values().stream().filter(c -> c >= 5).count();
        long mediumCount = userLoginCount.values().stream().filter(c -> c >= 1 && c < 5).count();
        long lowCount = totalUsers - highCount - mediumCount;

        // 高活跃
        DashboardVO.ActivityDistribution high = new DashboardVO.ActivityDistribution();
        high.setLevel("high");
        high.setCount(highCount);
        high.setPercentage(totalUsers > 0
                ? BigDecimal.valueOf(highCount * 100.0 / totalUsers).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO);
        distributions.add(high);

        // 中活跃
        DashboardVO.ActivityDistribution medium = new DashboardVO.ActivityDistribution();
        medium.setLevel("medium");
        medium.setCount(mediumCount);
        medium.setPercentage(totalUsers > 0
                ? BigDecimal.valueOf(mediumCount * 100.0 / totalUsers).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO);
        distributions.add(medium);

        // 低活跃
        DashboardVO.ActivityDistribution low = new DashboardVO.ActivityDistribution();
        low.setLevel("low");
        low.setCount(lowCount);
        low.setPercentage(totalUsers > 0
                ? BigDecimal.valueOf(lowCount * 100.0 / totalUsers).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO);
        distributions.add(low);

        return distributions;
    }

    /**
     * 按日/周/月统计用户、作品、浏览量增长
     */
    private List<DashboardVO.GrowthTrend> getGrowthTrends(String period) {
        List<DashboardVO.GrowthTrend> trends = new ArrayList<>();
        String dimension = (period != null) ? period : "daily";

        int periods;
        switch (dimension) {
            case "weekly" -> periods = 8;
            case "monthly" -> periods = 6;
            default -> periods = 7;
        }

        for (int i = periods - 1; i >= 0; i--) {
            LocalDateTime periodStart;
            LocalDateTime periodEnd;
            String label;

            switch (dimension) {
                case "weekly" -> {
                    periodStart = LocalDate.now().minusWeeks(i).atStartOfDay();
                    periodEnd = LocalDate.now().minusWeeks(i - 1).atStartOfDay();
                    label = periodStart.format(DateTimeFormatter.ofPattern("MM-dd")) + "~"
                            + periodEnd.minusDays(1).format(DateTimeFormatter.ofPattern("MM-dd"));
                }
                case "monthly" -> {
                    periodStart = LocalDate.now().minusMonths(i).atStartOfDay();
                    periodEnd = LocalDate.now().minusMonths(i - 1).atStartOfDay();
                    label = periodStart.format(DateTimeFormatter.ofPattern("yyyy-MM"));
                }
                default -> {
                    periodStart = LocalDate.now().minusDays(i).atStartOfDay();
                    periodEnd = LocalDate.now().minusDays(i - 1).atStartOfDay();
                    label = periodStart.format(DateTimeFormatter.ofPattern("MM-dd"));
                }
            }

            // 新增用户数
            long newUserCount = userMapper.selectCount(
                    new LambdaQueryWrapper<User>()
                            .ge(User::getCreatedAt, periodStart)
                            .lt(User::getCreatedAt, periodEnd));

            // 新增作品数
            long newArticleCount = articleMapper.selectCount(
                    new LambdaQueryWrapper<Article>()
                            .eq(Article::getStatus, ArticleStatusEnum.PUBLISHED)
                            .ge(Article::getCreatedAt, periodStart)
                            .lt(Article::getCreatedAt, periodEnd));

            // 浏览量
            long viewCount = articleViewHistoryMapper.selectCount(
                    new LambdaQueryWrapper<ArticleViewHistory>()
                            .ge(ArticleViewHistory::getCreateTime, periodStart)
                            .lt(ArticleViewHistory::getCreateTime, periodEnd));

            DashboardVO.GrowthTrend trend = new DashboardVO.GrowthTrend();
            trend.setPeriodLabel(label);
            trend.setDimension(dimension);
            trend.setNewUserCount(newUserCount);
            trend.setNewArticleCount(newArticleCount);
            trend.setViewCount(viewCount);
            trends.add(trend);
        }

        return trends;
    }

    /**
     * 按分类统计作品数量
     */
    private List<DashboardVO.CategoryDistribution> getCategoryDistributions() {
        List<DashboardVO.CategoryDistribution> distributions = new ArrayList<>();

        // 查询所有分类
        List<Category> categories = categoryMapper.selectList(
                new LambdaQueryWrapper<Category>().eq(Category::getStatus, 1));

        // 查询已发布作品按分类统计
        List<Article> publishedArticles = articleMapper.selectList(
                new LambdaQueryWrapper<Article>().eq(Article::getStatus, ArticleStatusEnum.PUBLISHED));

        Map<Long, Long> categoryArticleCount = publishedArticles.stream()
                .filter(a -> a.getCategoryId() != null)
                .collect(Collectors.groupingBy(Article::getCategoryId, Collectors.counting()));

        long totalArticles = publishedArticles.size();

        for (Category category : categories) {
            long count = categoryArticleCount.getOrDefault(category.getId(), 0L);
            if (count == 0) continue;

            DashboardVO.CategoryDistribution dist = new DashboardVO.CategoryDistribution();
            dist.setCategoryId(category.getId());
            dist.setCategoryName(category.getName());
            dist.setArticleCount(count);
            dist.setPercentage(totalArticles > 0
                    ? BigDecimal.valueOf(count * 100.0 / totalArticles).setScale(2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO);
            distributions.add(dist);
        }

        // 按作品数降序排列
        distributions.sort(Comparator.comparingLong(DashboardVO.CategoryDistribution::getArticleCount).reversed());

        return distributions;
    }

    /**
     * 按浏览量排序的热门作品（Top 10）
     */
    private List<DashboardVO.HotArticleRank> getHotArticleRanks() {
        List<Article> hotArticles = articleMapper.selectList(
                new LambdaQueryWrapper<Article>()
                        .eq(Article::getStatus, ArticleStatusEnum.PUBLISHED)
                        .orderByDesc(Article::getViewCount)
                        .last("LIMIT 10"));

        // 批量查询作者信息
        Set<Long> authorIds = hotArticles.stream()
                .map(Article::getAuthorId)
                .collect(Collectors.toSet());
        Map<Long, User> authorMap = authorIds.isEmpty() ? Collections.emptyMap()
                : userMapper.selectBatchIds(authorIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        List<DashboardVO.HotArticleRank> ranks = new ArrayList<>();
        int rank = 1;
        for (Article article : hotArticles) {
            DashboardVO.HotArticleRank hotRank = new DashboardVO.HotArticleRank();
            hotRank.setArticleId(article.getId());
            hotRank.setTitle(article.getTitle());
            hotRank.setViewCount(article.getViewCount());
            hotRank.setLikeCount(article.getLikeCount());
            hotRank.setCommentCount(article.getCommentCount());
            hotRank.setRank(rank++);

            User author = authorMap.get(article.getAuthorId());
            hotRank.setAuthorName(author != null ? author.getNickname() : null);

            ranks.add(hotRank);
        }

        return ranks;
    }

    /**
     * 按作品数排序的创作者（Top 10）
     */
    private List<DashboardVO.CreatorRank> getCreatorRanks() {
        // 查询已发布作品按作者统计
        List<Article> publishedArticles = articleMapper.selectList(
                new LambdaQueryWrapper<Article>().eq(Article::getStatus, ArticleStatusEnum.PUBLISHED));

        // 按作者分组统计作品数、总浏览量、总点赞数
        Map<Long, Long> authorArticleCount = publishedArticles.stream()
                .collect(Collectors.groupingBy(Article::getAuthorId, Collectors.counting()));
        Map<Long, Long> authorTotalViews = publishedArticles.stream()
                .collect(Collectors.groupingBy(Article::getAuthorId,
                        Collectors.summingLong(a -> a.getViewCount() != null ? a.getViewCount() : 0L)));
        Map<Long, Long> authorTotalLikes = publishedArticles.stream()
                .collect(Collectors.groupingBy(Article::getAuthorId,
                        Collectors.summingLong(a -> a.getLikeCount() != null ? a.getLikeCount() : 0L)));

        // 按作品数排序取前10
        List<Long> topAuthorIds = authorArticleCount.entrySet().stream()
                .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
                .limit(10)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        if (topAuthorIds.isEmpty()) {
            return Collections.emptyList();
        }

        // 批量查询用户信息
        Map<Long, User> userMap = userMapper.selectBatchIds(topAuthorIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        List<DashboardVO.CreatorRank> ranks = new ArrayList<>();
        int rank = 1;
        for (Long authorId : topAuthorIds) {
            User user = userMap.get(authorId);
            if (user == null) continue;

            DashboardVO.CreatorRank creatorRank = new DashboardVO.CreatorRank();
            creatorRank.setUserId(user.getId());
            creatorRank.setNickname(user.getNickname());
            creatorRank.setAvatar(user.getAvatar());
            creatorRank.setArticleCount(authorArticleCount.getOrDefault(authorId, 0L));
            creatorRank.setTotalViews(authorTotalViews.getOrDefault(authorId, 0L));
            creatorRank.setTotalLikes(authorTotalLikes.getOrDefault(authorId, 0L));
            creatorRank.setFollowerCount(user.getFollowerCount());
            creatorRank.setRank(rank++);
            ranks.add(creatorRank);
        }

        return ranks;
    }

    // ========== 群组管理 ==========

    @Override
    public PageResult<GroupVO> getGroupList(String keyword, Integer status, Integer page, Integer pageSize) {
        LambdaQueryWrapper<GroupInfo> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(GroupInfo::getName, keyword)
                    .or().like(GroupInfo::getGroupNumber, keyword));
        }
        if (status != null) {
            wrapper.eq(GroupInfo::getStatus, status);
        }
        wrapper.orderByDesc(GroupInfo::getCreatedAt);

        Page<GroupInfo> groupPage = new Page<>(page, pageSize);
        Page<GroupInfo> result = groupMapper.selectPage(groupPage, wrapper);

        Set<Long> ownerIds = result.getRecords().stream()
                .map(GroupInfo::getOwnerId).collect(Collectors.toSet());
        Map<Long, User> ownerMap = ownerIds.isEmpty() ? Collections.emptyMap()
                : userMapper.selectBatchIds(ownerIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        List<GroupVO> voList = result.getRecords().stream().map(g -> {
            GroupVO vo = new GroupVO();
            vo.setId(g.getId());
            vo.setName(g.getName());
            vo.setGroupNumber(g.getGroupNumber());
            vo.setAvatar(g.getAvatar());
            vo.setDescription(g.getDescription());
            vo.setOwnerId(g.getOwnerId());
            User owner = ownerMap.get(g.getOwnerId());
            vo.setOwnerName(owner != null ? owner.getNickname() : "");
            vo.setMemberCount(g.getMemberCount());
            vo.setMaxMembers(g.getMaxMembers());
            vo.setIsPublic(g.getIsPublic());
            vo.setStatus(g.getStatus());
            vo.setCreatedAt(g.getCreatedAt());
            vo.setUpdatedAt(g.getUpdatedAt());
            vo.setJoinedAt(g.getCreatedAt());
            return vo;
        }).collect(Collectors.toList());

        return new PageResult<>(voList, result.getTotal(), page, pageSize);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void toggleGroupStatus(Long adminId, Long groupId, Integer status) {
        GroupInfo group = groupMapper.selectById(groupId);
        if (group == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "群组不存在");
        }
        group.setStatus(status);
        groupMapper.updateById(group);

        String action = status == 0 ? "disable" : "enable";
        String ip = getClientIp();
        operationLogService.log(adminId, "群组管理", action, "group", groupId,
                (status == 0 ? "禁用" : "启用") + "群组：" + group.getName(), ip);
    }

    // ========== 私信监控 ==========

    @Override
    public PageResult<ConversationVO> getConversationList(Integer page, Integer pageSize) {
        // 获取所有私信消息，按会话分组
        LambdaQueryWrapper<UserMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(UserMessage::getCreatedAt);

        Page<UserMessage> msgPage = new Page<>(page, pageSize);
        // 使用 distinct 的方式获取会话
        // 简化实现：查询最近的消息并按 sender/receiver 对分组
        List<UserMessage> allMessages = userMessageMapper.selectList(wrapper);

        // 按会话对分组
        Map<String, List<UserMessage>> conversationMap = allMessages.stream()
                .collect(Collectors.groupingBy(m -> {
                    long min = Math.min(m.getSenderId(), m.getReceiverId());
                    long max = Math.max(m.getSenderId(), m.getReceiverId());
                    return min + "_" + max;
                }));

        Set<Long> userIds = new java.util.HashSet<>();
        allMessages.forEach(m -> {
            userIds.add(m.getSenderId());
            userIds.add(m.getReceiverId());
        });
        Map<Long, User> userMap = userIds.isEmpty() ? Collections.emptyMap()
                : userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        List<ConversationVO> voList = conversationMap.entrySet().stream()
                .map(entry -> {
                    List<UserMessage> msgs = entry.getValue();
                    UserMessage lastMsg = msgs.get(0);
                    String[] ids = entry.getKey().split("_");
                    long user1Id = Long.parseLong(ids[0]);
                    long user2Id = Long.parseLong(ids[1]);

                    ConversationVO vo = new ConversationVO();
                    vo.setId((long) entry.getKey().hashCode());
                    vo.setUser1Id(user1Id);
                    vo.setUser2Id(user2Id);
                    User user1 = userMap.get(user1Id);
                    User user2 = userMap.get(user2Id);
                    vo.setUser1Name(user1 != null ? user1.getNickname() : "用户" + user1Id);
                    vo.setUser2Name(user2 != null ? user2.getNickname() : "用户" + user2Id);
                    vo.setLastMessage(lastMsg.getContent());
                    vo.setMessageCount(msgs.size());
                    vo.setUnreadCount(0);
                    vo.setCreatedAt(msgs.get(msgs.size() - 1).getCreatedAt());
                    vo.setUpdatedAt(lastMsg.getCreatedAt());
                    return vo;
                })
                .sorted((a, b) -> {
                    if (a.getUpdatedAt() == null && b.getUpdatedAt() == null) return 0;
                    if (a.getUpdatedAt() == null) return 1;
                    if (b.getUpdatedAt() == null) return -1;
                    return b.getUpdatedAt().compareTo(a.getUpdatedAt());
                })
                .collect(Collectors.toList());

        long total = voList.size();
        // 手动分页
        int start = (page - 1) * pageSize;
        int end = Math.min(start + pageSize, voList.size());
        List<ConversationVO> pagedList = start < voList.size()
                ? voList.subList(start, end) : Collections.emptyList();

        return new PageResult<>(pagedList, total, page, pageSize);
    }

    @Override
    public PageResult<MessageVO> getConversationMessages(Long conversationId, Integer page, Integer pageSize) {
        // conversationId 实际上不使用，这里简化实现
        LambdaQueryWrapper<UserMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(UserMessage::getCreatedAt);

        Page<UserMessage> msgPage = new Page<>(page, pageSize);
        Page<UserMessage> result = userMessageMapper.selectPage(msgPage, wrapper);

        Set<Long> userIds = new java.util.HashSet<>();
        result.getRecords().forEach(m -> {
            userIds.add(m.getSenderId());
            userIds.add(m.getReceiverId());
        });
        Map<Long, User> userMap = userIds.isEmpty() ? Collections.emptyMap()
                : userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        List<MessageVO> voList = result.getRecords().stream().map(m -> {
            MessageVO vo = new MessageVO();
            vo.setId(m.getId());
            vo.setConversationId(m.getConversationId());
            vo.setSenderId(m.getSenderId());
            User sender = userMap.get(m.getSenderId());
            if (sender != null) {
                UserVO senderVO = new UserVO();
                senderVO.setId(sender.getId());
                senderVO.setNickname(sender.getNickname());
                senderVO.setAvatar(sender.getAvatar());
                vo.setSender(senderVO);
            }
            vo.setContent(m.getContent());
            vo.setType(m.getType());
            vo.setCreatedAt(m.getCreatedAt());
            return vo;
        }).collect(Collectors.toList());

        return new PageResult<>(voList, result.getTotal(), page, pageSize);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteMessageAsAdmin(Long adminId, Long messageId) {
        UserMessage message = userMessageMapper.selectById(messageId);
        if (message == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "消息不存在");
        }
        userMessageMapper.deleteById(messageId);

        String ip = getClientIp();
        operationLogService.log(adminId, "私信管理", "delete", "message", messageId,
                "删除私信消息", ip);
    }

    // ========== 登录日志 ==========

    @Override
    public PageResult<LoginLog> getLoginLogs(String keyword, Integer status, String startDate,
                                              String endDate, Integer page, Integer pageSize) {
        LambdaQueryWrapper<LoginLog> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(LoginLog::getUsername, keyword));
        }
        if (status != null) {
            wrapper.eq(LoginLog::getStatus, status);
        }
        if (StringUtils.hasText(startDate)) {
            LocalDateTime start = LocalDate.parse(startDate).atStartOfDay();
            wrapper.ge(LoginLog::getCreatedAt, start);
        }
        if (StringUtils.hasText(endDate)) {
            LocalDateTime end = LocalDate.parse(endDate).plusDays(1).atStartOfDay();
            wrapper.lt(LoginLog::getCreatedAt, end);
        }
        wrapper.orderByDesc(LoginLog::getCreatedAt);

        Page<LoginLog> logPage = new Page<>(page, pageSize);
        Page<LoginLog> result = loginLogMapper.selectPage(logPage, wrapper);

        return new PageResult<>(result.getRecords(), result.getTotal(), page, pageSize);
    }

    // ========== 通知管理 ==========

    @Override
    public PageResult<NotificationVO> getNotificationList(Integer type, Integer page, Integer pageSize) {
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        if (type != null) {
            wrapper.eq(Notification::getType, type);
        }
        wrapper.orderByDesc(Notification::getCreatedAt);

        Page<Notification> notifPage = new Page<>(page, pageSize);
        Page<Notification> result = notificationMapper.selectPage(notifPage, wrapper);

        List<NotificationVO> voList = result.getRecords().stream().map(n -> {
            NotificationVO vo = new NotificationVO();
            vo.setId(n.getId());
            vo.setType(n.getType().getValue());
            vo.setTitle(n.getTitle());
            vo.setContent(n.getContent());
            vo.setCreatedAt(n.getCreatedAt());
            return vo;
        }).collect(Collectors.toList());

        return new PageResult<>(voList, result.getTotal(), page, pageSize);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sendNotification(Long adminId, Integer type, String title, String content,
                                  Boolean targetAll, List<Long> targetUserIds) {
        if (Boolean.TRUE.equals(targetAll)) {
            // 发送给所有用户
            List<User> allUsers = userMapper.selectList(new LambdaQueryWrapper<>());
            for (User user : allUsers) {
                try {
                    notificationService.createNotification(user.getId(), type, title, content, null);
                } catch (Exception e) {
                    log.error("发送通知给用户 {} 失败: {}", user.getId(), e.getMessage());
                }
            }
        } else if (targetUserIds != null && !targetUserIds.isEmpty()) {
            for (Long userId : targetUserIds) {
                try {
                    notificationService.createNotification(userId, type, title, content, null);
                } catch (Exception e) {
                    log.error("发送通知给用户 {} 失败: {}", userId, e.getMessage());
                }
            }
        }

        String ip = getClientIp();
        operationLogService.log(adminId, "通知管理", "send", "notification", null,
                "发送通知：" + title + "，目标：" + (Boolean.TRUE.equals(targetAll) ? "全部用户" : targetUserIds), ip);
    }

    // ========== 用户登录历史 ==========

    @Override
    public PageResult<LoginLog> getUserLoginHistory(Long userId, Integer page, Integer pageSize) {
        LambdaQueryWrapper<LoginLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LoginLog::getUserId, userId);
        wrapper.orderByDesc(LoginLog::getCreatedAt);

        Page<LoginLog> logPage = new Page<>(page, pageSize);
        Page<LoginLog> result = loginLogMapper.selectPage(logPage, wrapper);

        return new PageResult<>(result.getRecords(), result.getTotal(), page, pageSize);
    }

    // ========== 群组成员管理 ==========

    @Override
    public PageResult<GroupMember> getGroupMembers(Long groupId, Integer page, Integer pageSize) {
        LambdaQueryWrapper<GroupMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GroupMember::getGroupId, groupId);
        wrapper.orderByAsc(GroupMember::getRole).orderByAsc(GroupMember::getJoinedAt);

        Page<GroupMember> memberPage = new Page<>(page, pageSize);
        Page<GroupMember> result = groupMemberMapper.selectPage(memberPage, wrapper);

        // 填充用户昵称和头像
        Set<Long> userIds = result.getRecords().stream()
                .map(GroupMember::getUserId).collect(Collectors.toSet());
        Map<Long, User> userMap = userIds.isEmpty() ? Collections.emptyMap()
                : userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        result.getRecords().forEach(m -> {
            User user = userMap.get(m.getUserId());
            if (user != null) {
                m.setUserName(user.getUsername());
                m.setUserAvatar(user.getAvatar());
            }
        });

        return new PageResult<>(result.getRecords(), result.getTotal(), page, pageSize);
    }

    // ========== 群组消息管理 ==========

    @Override
    public PageResult<GroupMessageVO> getGroupMessagesAsAdmin(Long groupId, Integer page, Integer pageSize) {
        LambdaQueryWrapper<GroupMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GroupMessage::getGroupId, groupId);
        wrapper.orderByDesc(GroupMessage::getCreatedAt);

        Page<GroupMessage> msgPage = new Page<>(page, pageSize);
        Page<GroupMessage> result = groupMessageMapper.selectPage(msgPage, wrapper);

        // 填充发送者信息
        Set<Long> senderIds = result.getRecords().stream()
                .map(GroupMessage::getSenderId).collect(Collectors.toSet());
        Map<Long, User> userMap = senderIds.isEmpty() ? Collections.emptyMap()
                : userMapper.selectBatchIds(senderIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        List<GroupMessageVO> voList = result.getRecords().stream().map(m -> {
            GroupMessageVO vo = new GroupMessageVO();
            vo.setId(m.getId());
            vo.setGroupId(m.getGroupId());
            vo.setSenderId(m.getSenderId());
            User sender = userMap.get(m.getSenderId());
            vo.setSenderName(sender != null ? sender.getNickname() : "");
            vo.setSenderAvatar(sender != null ? sender.getAvatar() : "");
            vo.setContent(m.getContent());
            vo.setMessageType(m.getMessageType());
            vo.setCreatedAt(m.getCreatedAt());
            return vo;
        }).collect(Collectors.toList());

        return new PageResult<>(voList, result.getTotal(), page, pageSize);
    }

    // ========== AI 使用统计 ==========

    @Override
    public Map<String, Object> getAIUsageStats(String period) {
        Map<String, Object> stats = new HashMap<>();
        // 基础统计（从操作日志中统计 AI 相关操作）
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OperationLog::getModule, "AI写作");

        LocalDate today = LocalDate.now();
        if ("daily".equals(period)) {
            wrapper.ge(OperationLog::getCreatedAt, today.atStartOfDay());
        } else if ("weekly".equals(period)) {
            wrapper.ge(OperationLog::getCreatedAt, today.minusDays(7).atStartOfDay());
        } else if ("monthly".equals(period)) {
            wrapper.ge(OperationLog::getCreatedAt, today.minusDays(30).atStartOfDay());
        }

        long totalRequests = operationLogMapper.selectCount(wrapper);

        stats.put("totalRequests", totalRequests);
        stats.put("todayRequests", totalRequests);
        stats.put("textGenerationCount", totalRequests);
        stats.put("imageGenerationCount", 0L);
        stats.put("totalTokens", 0L);
        stats.put("averageResponseTime", 0.0);
        stats.put("userCount", 0L);
        stats.put("dailyStats", Collections.emptyList());
        stats.put("topUsers", Collections.emptyList());

        return stats;
    }

    // ========== 协作管理 ==========

    @Override
    public PageResult<CollaboratorVO> getCollaborationList(Integer page, Integer pageSize) {
        LambdaQueryWrapper<ArticleCollaborator> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(ArticleCollaborator::getCreatedAt);

        Page<ArticleCollaborator> collabPage = new Page<>(page, pageSize);
        Page<ArticleCollaborator> result = articleCollaboratorMapper.selectPage(collabPage, wrapper);

        // 填充用户和文章信息
        Set<Long> userIds = new java.util.HashSet<>();
        Set<Long> articleIds = new java.util.HashSet<>();
        result.getRecords().forEach(c -> {
            userIds.add(c.getInvitedBy());
            userIds.add(c.getUserId());
            articleIds.add(c.getArticleId());
        });

        Map<Long, User> userMap = userIds.isEmpty() ? Collections.emptyMap()
                : userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));
        Map<Long, Article> articleMap = articleIds.isEmpty() ? Collections.emptyMap()
                : articleMapper.selectBatchIds(articleIds).stream()
                .collect(Collectors.toMap(Article::getId, a -> a));

        List<CollaboratorVO> voList = result.getRecords().stream().map(c -> {
            CollaboratorVO vo = new CollaboratorVO();
            vo.setId(c.getId());
            vo.setArticleId(c.getArticleId());
            Article article = articleMap.get(c.getArticleId());
            vo.setArticleTitle(article != null ? article.getTitle() : "");
            vo.setInviterId(c.getInvitedBy());
            User inviter = userMap.get(c.getInvitedBy());
            vo.setInviterName(inviter != null ? inviter.getNickname() : "");
            vo.setInviteeId(c.getUserId());
            User invitee = userMap.get(c.getUserId());
            vo.setInviteeName(invitee != null ? invitee.getNickname() : "");
            vo.setPermission(c.getPermission());
            vo.setStatus(c.getStatus());
            vo.setCreatedAt(c.getCreatedAt());
            vo.setUpdatedAt(c.getUpdatedAt());
            return vo;
        }).collect(Collectors.toList());

        return new PageResult<>(voList, result.getTotal(), page, pageSize);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCollaboration(Long adminId, Long id) {
        ArticleCollaborator collab = articleCollaboratorMapper.selectById(id);
        if (collab == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "协作关系不存在");
        }
        articleCollaboratorMapper.deleteById(id);

        String ip = getClientIp();
        operationLogService.log(adminId, "协作管理", "delete", "collaboration", id,
                "删除协作关系", ip);
    }

    // ========== 标签管理（分页） ==========

    @Override
    public PageResult<com.zhixun.entity.Tag> getTagList(String keyword, String sortBy,
                                                        Integer page, Integer pageSize) {
        LambdaQueryWrapper<com.zhixun.entity.Tag> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(keyword)) {
            wrapper.like(com.zhixun.entity.Tag::getName, keyword);
        }

        // 排序
        if ("articleCountDesc".equals(sortBy)) {
            wrapper.orderByDesc(com.zhixun.entity.Tag::getArticleCount);
        } else if ("articleCountAsc".equals(sortBy)) {
            wrapper.orderByAsc(com.zhixun.entity.Tag::getArticleCount);
        } else {
            wrapper.orderByDesc(com.zhixun.entity.Tag::getCreatedAt);
        }

        Page<com.zhixun.entity.Tag> tagPage = new Page<>(page, pageSize);
        Page<com.zhixun.entity.Tag> result = tagMapper.selectPage(tagPage, wrapper);

        return new PageResult<>(result.getRecords(), result.getTotal(), page, pageSize);
    }

    // ========== 管理员管理（超级管理员专属） ==========

    @Override
    public PageResult<UserVO> getAdminList(String keyword, Integer status, Integer page, Integer pageSize) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(User::getRole, RoleEnum.ADMIN, RoleEnum.SUPER_ADMIN);

        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(User::getUsername, keyword)
                    .or().like(User::getNickname, keyword));
        }
        if (status != null) {
            wrapper.eq(User::getStatus, status);
        }
        wrapper.orderByAsc(User::getCreatedAt);

        Page<User> userPage = userMapper.selectPage(new Page<>(page, pageSize), wrapper);
        List<UserVO> voList = userPage.getRecords().stream()
                .map(this::convertToUserVO)
                .toList();
        return new PageResult<>(voList, userPage.getTotal(), page, pageSize);
    }

    @Override
    @Transactional
    public void updateAdminRole(Long operatorId, Long targetId, String newRole) {
        // 不能修改自己的角色
        if (operatorId.equals(targetId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "不能修改自己的角色");
        }

        User target = userMapper.selectById(targetId);
        if (target == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }

        // 只能修改 ADMIN 或 SUPER_ADMIN 的角色
        if (target.getRole() != RoleEnum.ADMIN && target.getRole() != RoleEnum.SUPER_ADMIN) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "只能修改管理员的角色");
        }

        RoleEnum roleEnum;
        if ("ADMIN".equals(newRole)) {
            roleEnum = RoleEnum.ADMIN;
        } else if ("SUPER_ADMIN".equals(newRole)) {
            roleEnum = RoleEnum.SUPER_ADMIN;
        } else {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "无效的角色: " + newRole);
        }

        LambdaUpdateWrapper<User> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(User::getId, targetId).set(User::getRole, roleEnum);
        userMapper.update(null, updateWrapper);

        operationLogService.log(operatorId, "角色管理", "修改角色",
                "user", targetId, "newRole=" + newRole, getClientIp());
    }

    @Override
    @Transactional
    public void updateAdminStatus(Long operatorId, Long targetId, Integer status) {
        // 不能禁用自己的账号
        if (operatorId.equals(targetId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "不能操作自己的账号");
        }

        User target = userMapper.selectById(targetId);
        if (target == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }

        // 只能操作 ADMIN 或 SUPER_ADMIN 的状态
        if (target.getRole() != RoleEnum.ADMIN && target.getRole() != RoleEnum.SUPER_ADMIN) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "只能操作管理员账号");
        }

        LambdaUpdateWrapper<User> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(User::getId, targetId).set(User::getStatus, status);
        userMapper.update(null, updateWrapper);

        operationLogService.log(operatorId, "管理员管理", status == 1 ? "启用管理员" : "禁用管理员",
                "user", targetId, "status=" + status, getClientIp());
    }

    private UserVO convertToUserVO(User user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUid(user.getUid());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setRole(user.getRole() != null ? user.getRole().getValue() : "USER");
        vo.setStatus(user.getStatus());
        vo.setCreatedAt(user.getCreatedAt());
        vo.setArticleCount(user.getArticleCount() != null ? user.getArticleCount() : 0);
        vo.setFollowCount(user.getFollowCount() != null ? user.getFollowCount() : 0);
        vo.setFollowerCount(user.getFollowerCount() != null ? user.getFollowerCount() : 0);
        return vo;
    }
}
