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
import com.zhixun.entity.ArticleLike;
import com.zhixun.entity.ArticleTag;
import com.zhixun.entity.ArticleViewHistory;
import com.zhixun.entity.Category;
import com.zhixun.entity.Comment;
import com.zhixun.entity.LoginLog;
import com.zhixun.entity.Notification;
import com.zhixun.entity.OperationLog;
import com.zhixun.entity.SecurityAuditLog;
import com.zhixun.entity.SensitiveWhitelist;
import com.zhixun.entity.SensitiveWord;
import com.zhixun.entity.Tag;
import com.zhixun.entity.User;
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
import com.zhixun.mapper.CategoryMapper;
import com.zhixun.mapper.CommentMapper;
import com.zhixun.mapper.LoginLogMapper;
import com.zhixun.mapper.NotificationMapper;
import com.zhixun.mapper.OperationLogMapper;
import com.zhixun.mapper.SecurityAuditLogMapper;
import com.zhixun.mapper.SensitiveWhitelistMapper;
import com.zhixun.mapper.SensitiveWordMapper;
import com.zhixun.mapper.TagMapper;
import com.zhixun.mapper.UserMapper;
import com.zhixun.mapper.ViewHistoryMapper;
import com.zhixun.common.util.SensitiveWordUtil;
import com.zhixun.service.AdminService;
import com.zhixun.service.NotificationService;
import com.zhixun.service.OperationLogService;
import com.zhixun.vo.ArticleVO;
import com.zhixun.vo.CommentVO;
import com.zhixun.vo.DashboardVO;
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
            throw new BusinessException(ErrorCode.NOT_FOUND, "文章不存在");
        }

        // 审核状态只能从待审核变更
        if (article.getStatus() != ArticleStatusEnum.PENDING) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "文章当前状态不允许审核");
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
        operationLogService.log(adminId, "文章审核", action, "article", articleId, detail, ip);

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

        LocalDateTime todayStart = LocalDate.now().atStartOfDay();

        // 用户总量
        vo.setUserTotal(userMapper.selectCount(
                new LambdaQueryWrapper<>()));

        // 文章总量
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

        // 用户留存率（查询注册用户在后续7天内的登录留存率）
        vo.setRetentionRates(getRetentionRates());

        // 用户活跃度分布
        vo.setActivityDistributions(getActivityDistributions());

        // 增长趋势数据（按时间维度）
        vo.setGrowthTrends(getGrowthTrends(period));

        // 分类文章分布
        vo.setCategoryDistributions(getCategoryDistributions());

        // 热门文章排行
        vo.setHotArticleRanks(getHotArticleRanks());

        // 创作者排行
        vo.setCreatorRanks(getCreatorRanks());

        return vo;
    }

    @Override
    public PageResult<UserVO> getUserList(String keyword, String role, Integer status, Integer page, Integer pageSize) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();

        // 关键词搜索
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(User::getUsername, keyword)
                    .or().like(User::getNickname, keyword)
                    .or().like(User::getEmail, keyword));
        }

        // 角色筛选
        if (StringUtils.hasText(role)) {
            wrapper.eq(User::getRole, RoleEnum.valueOf(role));
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

        // 文章ID筛选
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

        // 减少文章评论数（仅当评论之前是正常状态时才减少）
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
                title = "文章审核通过";
                content = "您的文章已通过审核，现已发布";
            } else {
                title = "文章审核驳回";
                content = "您的文章未通过审核，原因：" + (reason != null ? reason : "无");
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
     * 批量将文章实体列表转换为 VO 列表
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

        // 按用户分组统计登录次数
        Map<Long, Long> userLoginCount = recentLogs.stream()
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
     * 按日/周/月统计用户、文章、浏览量增长
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

            // 新增文章数
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
     * 按分类统计文章数量
     */
    private List<DashboardVO.CategoryDistribution> getCategoryDistributions() {
        List<DashboardVO.CategoryDistribution> distributions = new ArrayList<>();

        // 查询所有分类
        List<Category> categories = categoryMapper.selectList(
                new LambdaQueryWrapper<Category>().eq(Category::getStatus, 1));

        // 查询已发布文章按分类统计
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

        // 按文章数降序排列
        distributions.sort(Comparator.comparingLong(DashboardVO.CategoryDistribution::getArticleCount).reversed());

        return distributions;
    }

    /**
     * 按浏览量排序的热门文章（Top 10）
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
     * 按文章数排序的创作者（Top 10）
     */
    private List<DashboardVO.CreatorRank> getCreatorRanks() {
        // 查询已发布文章按作者统计
        List<Article> publishedArticles = articleMapper.selectList(
                new LambdaQueryWrapper<Article>().eq(Article::getStatus, ArticleStatusEnum.PUBLISHED));

        // 按作者分组统计文章数、总浏览量、总点赞数
        Map<Long, Long> authorArticleCount = publishedArticles.stream()
                .collect(Collectors.groupingBy(Article::getAuthorId, Collectors.counting()));
        Map<Long, Long> authorTotalViews = publishedArticles.stream()
                .collect(Collectors.groupingBy(Article::getAuthorId,
                        Collectors.summingLong(a -> a.getViewCount() != null ? a.getViewCount() : 0L)));
        Map<Long, Long> authorTotalLikes = publishedArticles.stream()
                .collect(Collectors.groupingBy(Article::getAuthorId,
                        Collectors.summingLong(a -> a.getLikeCount() != null ? a.getLikeCount() : 0L)));

        // 按文章数排序取前10
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
}
