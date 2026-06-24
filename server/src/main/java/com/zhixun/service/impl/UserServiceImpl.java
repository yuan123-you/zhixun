package com.zhixun.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhixun.common.exception.BusinessException;
import com.zhixun.common.result.ErrorCode;
import com.zhixun.common.result.PageResult;
import com.zhixun.common.util.AesUtil;
import com.zhixun.config.Slave;
import com.zhixun.dto.user.ProfileUpdateRequest;
import com.zhixun.dto.user.SettingsUpdateRequest;
import com.zhixun.entity.Article;
import com.zhixun.entity.ArticleLike;
import com.zhixun.entity.User;
import com.zhixun.entity.UserPreferredCategory;
import com.zhixun.entity.UserPreferredTag;
import com.zhixun.entity.UserSettings;
import com.zhixun.entity.ViewHistory;
import com.zhixun.enums.ArticleStatusEnum;
import com.zhixun.enums.LikeTargetTypeEnum;
import com.zhixun.enums.PreferredTypeEnum;
import com.zhixun.mapper.ArticleLikeMapper;
import com.zhixun.mapper.ArticleMapper;
import com.zhixun.mapper.UserMapper;
import com.zhixun.mapper.UserPreferredCategoryMapper;
import com.zhixun.mapper.UserPreferredTagMapper;
import com.zhixun.mapper.UserSettingsMapper;
import com.zhixun.mapper.ViewHistoryMapper;
import com.zhixun.service.UserService;
import com.zhixun.service.OnlineStatusService;
import com.zhixun.service.OpenSearchSyncService;
import com.zhixun.vo.ArticleVO;
import com.zhixun.vo.TagVO;
import com.zhixun.vo.UserSettingsVO;
import com.zhixun.vo.UserVO;
import com.zhixun.entity.ArticleTag;
import com.zhixun.entity.Tag;
import com.zhixun.entity.Category;
import com.zhixun.mapper.ArticleTagMapper;
import com.zhixun.mapper.TagMapper;
import com.zhixun.mapper.CategoryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final ArticleMapper articleMapper;
    private final ArticleLikeMapper articleLikeMapper;
    private final ViewHistoryMapper viewHistoryMapper;
    private final UserSettingsMapper userSettingsMapper;
    private final UserPreferredCategoryMapper userPreferredCategoryMapper;
    private final UserPreferredTagMapper userPreferredTagMapper;
    private final ArticleTagMapper articleTagMapper;
    private final TagMapper tagMapper;
    private final CategoryMapper categoryMapper;
    private final AesUtil aesUtil;
    private final OpenSearchSyncService openSearchSyncService;
    private final OnlineStatusService onlineStatusService;

    /** Redis 模板（可选，Redis 不可用时为 null） */
    @Autowired(required = false)
    private StringRedisTemplate stringRedisTemplate;

    /** 浏览量 Redis Key 前缀 */
    private static final String VIEW_COUNT_PREFIX = "article:view:";

    @Override
    @Slave
    public UserVO getProfile(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return buildUserVO(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProfile(Long userId, ProfileUpdateRequest request) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        // 更新昵称
        if (StringUtils.hasText(request.getNickname())) {
            user.setNickname(request.getNickname());
        }

        // 更新头像
        if (StringUtils.hasText(request.getAvatar())) {
            user.setAvatar(request.getAvatar());
        }

        // 更新邮箱（AES 加密存储）
        if (StringUtils.hasText(request.getEmail())) {
            user.setEmail(aesUtil.encrypt(request.getEmail()));
        }

        // 更新手机号（AES 加密存储）
        if (StringUtils.hasText(request.getPhone())) {
            user.setPhone(aesUtil.encrypt(request.getPhone()));
        }

        // 更新简介（允许清空）
        if (request.getBio() != null) {
            user.setBio(request.getBio());
        }

        userMapper.updateById(user);

        // 同步到 OpenSearch（非关键操作，失败不影响主事务）
        try {
            openSearchSyncService.syncUser(userId);
        } catch (Exception e) {
            log.error("更新用户资料同步OpenSearch失败, userId={}: {}", userId, e.getMessage());
        }
    }

    @Override
    @Slave
    public PageResult<ArticleVO> getUserArticles(Long userId, Integer status, Integer page, Integer pageSize) {
        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Article::getAuthorId, userId);

        if (status != null) {
            wrapper.eq(Article::getStatus, ArticleStatusEnum.fromValue(status));
        }

        wrapper.orderByDesc(Article::getCreatedAt);

        Page<Article> articlePage = new Page<>(page, pageSize);
        Page<Article> result = articleMapper.selectPage(articlePage, wrapper);

        List<ArticleVO> voList = convertToVOList(result.getRecords());

        return new PageResult<>(voList, result.getTotal(), page, pageSize);
    }

    @Override
    @Slave
    public PageResult<ArticleVO> getUserLikes(Long userId, Integer page, Integer pageSize) {
        // 查询用户点赞的文章类型记录
        LambdaQueryWrapper<ArticleLike> likeWrapper = new LambdaQueryWrapper<>();
        likeWrapper.eq(ArticleLike::getUserId, userId)
                .eq(ArticleLike::getTargetType, LikeTargetTypeEnum.ARTICLE)
                .orderByDesc(ArticleLike::getCreatedAt);
        Page<ArticleLike> likePage = new Page<>(page, pageSize);
        Page<ArticleLike> likeResult = articleLikeMapper.selectPage(likePage, likeWrapper);

        if (likeResult.getRecords().isEmpty()) {
            return new PageResult<>(Collections.emptyList(), 0L, page, pageSize);
        }

        List<Long> articleIds = likeResult.getRecords().stream()
                .map(ArticleLike::getTargetId)
                .collect(Collectors.toList());

        List<Article> articles = articleMapper.selectBatchIds(articleIds);
        List<ArticleVO> voList = convertToVOList(articles);

        return new PageResult<>(voList, likeResult.getTotal(), page, pageSize);
    }

    @Override
    @Slave
    public PageResult<ArticleVO> getViewHistory(Long userId, String startDate, String endDate, Integer page, Integer pageSize) {
        LambdaQueryWrapper<ViewHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ViewHistory::getUserId, userId);

        // 日期范围筛选
        if (StringUtils.hasText(startDate)) {
            LocalDateTime start = LocalDate.parse(startDate).atStartOfDay();
            wrapper.ge(ViewHistory::getCreatedAt, start);
        }
        if (StringUtils.hasText(endDate)) {
            LocalDateTime end = LocalDate.parse(endDate).atTime(LocalTime.MAX);
            wrapper.le(ViewHistory::getCreatedAt, end);
        }

        wrapper.orderByDesc(ViewHistory::getCreatedAt);

        Page<ViewHistory> historyPage = new Page<>(page, pageSize);
        Page<ViewHistory> result = viewHistoryMapper.selectPage(historyPage, wrapper);

        if (result.getRecords().isEmpty()) {
            return new PageResult<>(Collections.emptyList(), 0L, page, pageSize);
        }

        List<Long> articleIds = result.getRecords().stream()
                .map(ViewHistory::getArticleId)
                .distinct()
                .collect(Collectors.toList());

        List<Article> articles = articleMapper.selectBatchIds(articleIds);
        List<ArticleVO> voList = convertToVOList(articles);

        return new PageResult<>(voList, result.getTotal(), page, pageSize);
    }

    @Override
    @Slave
    public UserSettingsVO getSettings(Long userId) {
        UserSettingsVO vo = new UserSettingsVO();

        // 查询用户设置
        LambdaQueryWrapper<UserSettings> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserSettings::getUserId, userId);
        UserSettings settings = userSettingsMapper.selectOne(wrapper);

        // 构建 notification
        UserSettingsVO.Notification notification = new UserSettingsVO.Notification();
        if (settings != null) {
            notification.setNotifySystem(settings.getNotifySystem());
            notification.setNotifyInteract(settings.getNotifyInteract());
            notification.setNotifyMessage(settings.getNotifyMessage());
            notification.setNotifyFollow(settings.getNotifyFollow());
        } else {
            notification.setNotifySystem(1);
            notification.setNotifyInteract(1);
            notification.setNotifyMessage(1);
            notification.setNotifyFollow(1);
        }
        vo.setNotification(notification);

        // 构建 privacy
        UserSettingsVO.Privacy privacy = new UserSettingsVO.Privacy();
        if (settings != null) {
            privacy.setShowOnlineStatus(settings.getShowOnlineStatus());
            privacy.setMessagePermission(settings.getMessagePermission());
            privacy.setSaveViewHistory(settings.getSaveViewHistory());
        } else {
            privacy.setShowOnlineStatus(1);
            privacy.setMessagePermission(0);
            privacy.setSaveViewHistory(1);
        }
        vo.setPrivacy(privacy);

        // 构建 display
        UserSettingsVO.Display display = new UserSettingsVO.Display();
        if (settings != null) {
            display.setFontSize(settings.getFontSize());
            display.setTheme(settings.getTheme());
            display.setLanguage(settings.getLanguage());
        } else {
            display.setFontSize(1);
            display.setTheme("light");
            display.setLanguage("zh-CN");
        }
        vo.setDisplay(display);

        // 构建 recommend（从 user_preferred_category 和 user_preferred_tag 表查询）
        UserSettingsVO.Recommend recommend = new UserSettingsVO.Recommend();

        // 感兴趣的分类
        LambdaQueryWrapper<UserPreferredCategory> interestedCatWrapper = new LambdaQueryWrapper<>();
        interestedCatWrapper.eq(UserPreferredCategory::getUserId, userId)
                .eq(UserPreferredCategory::getType, PreferredTypeEnum.INTERESTED);
        List<UserPreferredCategory> interestedCategories = userPreferredCategoryMapper.selectList(interestedCatWrapper);
        recommend.setInterestedCategories(interestedCategories.stream()
                .map(UserPreferredCategory::getCategoryId)
                .collect(Collectors.toList()));

        // 屏蔽的分类
        LambdaQueryWrapper<UserPreferredCategory> blockedCatWrapper = new LambdaQueryWrapper<>();
        blockedCatWrapper.eq(UserPreferredCategory::getUserId, userId)
                .eq(UserPreferredCategory::getType, PreferredTypeEnum.BLOCKED);
        List<UserPreferredCategory> blockedCategories = userPreferredCategoryMapper.selectList(blockedCatWrapper);
        recommend.setBlockedCategories(blockedCategories.stream()
                .map(UserPreferredCategory::getCategoryId)
                .collect(Collectors.toList()));

        // 感兴趣的标签
        LambdaQueryWrapper<UserPreferredTag> interestedTagWrapper = new LambdaQueryWrapper<>();
        interestedTagWrapper.eq(UserPreferredTag::getUserId, userId)
                .eq(UserPreferredTag::getType, PreferredTypeEnum.INTERESTED);
        List<UserPreferredTag> interestedTags = userPreferredTagMapper.selectList(interestedTagWrapper);
        recommend.setInterestedTags(interestedTags.stream()
                .map(UserPreferredTag::getTagId)
                .collect(Collectors.toList()));

        // 屏蔽的标签
        LambdaQueryWrapper<UserPreferredTag> blockedTagWrapper = new LambdaQueryWrapper<>();
        blockedTagWrapper.eq(UserPreferredTag::getUserId, userId)
                .eq(UserPreferredTag::getType, PreferredTypeEnum.BLOCKED);
        List<UserPreferredTag> blockedTags = userPreferredTagMapper.selectList(blockedTagWrapper);
        recommend.setBlockedTags(blockedTags.stream()
                .map(UserPreferredTag::getTagId)
                .collect(Collectors.toList()));

        vo.setRecommend(recommend);

        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSettings(Long userId, SettingsUpdateRequest request) {
        // 查询或创建用户设置
        LambdaQueryWrapper<UserSettings> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserSettings::getUserId, userId);
        UserSettings settings = userSettingsMapper.selectOne(wrapper);

        if (settings == null) {
            settings = new UserSettings();
            settings.setUserId(userId);
            // 设置默认值
            settings.setNotifySystem(1);
            settings.setNotifyInteract(1);
            settings.setNotifyMessage(1);
            settings.setNotifyFollow(1);
            settings.setShowOnlineStatus(1);
            settings.setMessagePermission(0);
            settings.setSaveViewHistory(1);
            settings.setFontSize(1);
            settings.setTheme("light");
            settings.setLanguage("zh-CN");
        }

        // 更新 notification 设置
        if (request.getNotification() != null) {
            SettingsUpdateRequest.Notification notification = request.getNotification();
            if (notification.getNotifySystem() != null) {
                settings.setNotifySystem(notification.getNotifySystem());
            }
            if (notification.getNotifyInteract() != null) {
                settings.setNotifyInteract(notification.getNotifyInteract());
            }
            if (notification.getNotifyMessage() != null) {
                settings.setNotifyMessage(notification.getNotifyMessage());
            }
            if (notification.getNotifyFollow() != null) {
                settings.setNotifyFollow(notification.getNotifyFollow());
            }
        }

        // 更新 privacy 设置
        if (request.getPrivacy() != null) {
            SettingsUpdateRequest.Privacy privacy = request.getPrivacy();
            if (privacy.getShowOnlineStatus() != null) {
                settings.setShowOnlineStatus(privacy.getShowOnlineStatus());
                // 清除在线状态可见性缓存
                onlineStatusService.invalidateShowOnlineStatusCache(userId);
            }
            if (privacy.getMessagePermission() != null) {
                settings.setMessagePermission(privacy.getMessagePermission());
            }
            if (privacy.getSaveViewHistory() != null) {
                settings.setSaveViewHistory(privacy.getSaveViewHistory());
            }
        }

        // 更新 display 设置
        if (request.getDisplay() != null) {
            SettingsUpdateRequest.Display display = request.getDisplay();
            if (display.getFontSize() != null) {
                settings.setFontSize(display.getFontSize());
            }
            if (display.getTheme() != null) {
                settings.setTheme(display.getTheme());
            }
            if (display.getLanguage() != null) {
                settings.setLanguage(display.getLanguage());
            }
        }

        // 保存 user_settings
        if (settings.getId() == null) {
            userSettingsMapper.insert(settings);
        } else {
            userSettingsMapper.updateById(settings);
        }

        // 更新 recommend（偏好分类和标签）
        if (request.getRecommend() != null) {
            SettingsUpdateRequest.Recommend recommend = request.getRecommend();

            // 更新感兴趣的分类：先删后插
            if (recommend.getInterestedCategories() != null) {
                LambdaQueryWrapper<UserPreferredCategory> delWrapper = new LambdaQueryWrapper<>();
                delWrapper.eq(UserPreferredCategory::getUserId, userId)
                        .eq(UserPreferredCategory::getType, PreferredTypeEnum.INTERESTED);
                userPreferredCategoryMapper.delete(delWrapper);

                for (Long categoryId : recommend.getInterestedCategories()) {
                    UserPreferredCategory entity = new UserPreferredCategory();
                    entity.setUserId(userId);
                    entity.setCategoryId(categoryId);
                    entity.setType(PreferredTypeEnum.INTERESTED);
                    userPreferredCategoryMapper.insert(entity);
                }
            }

            // 更新屏蔽的分类：先删后插
            if (recommend.getBlockedCategories() != null) {
                LambdaQueryWrapper<UserPreferredCategory> delWrapper = new LambdaQueryWrapper<>();
                delWrapper.eq(UserPreferredCategory::getUserId, userId)
                        .eq(UserPreferredCategory::getType, PreferredTypeEnum.BLOCKED);
                userPreferredCategoryMapper.delete(delWrapper);

                for (Long categoryId : recommend.getBlockedCategories()) {
                    UserPreferredCategory entity = new UserPreferredCategory();
                    entity.setUserId(userId);
                    entity.setCategoryId(categoryId);
                    entity.setType(PreferredTypeEnum.BLOCKED);
                    userPreferredCategoryMapper.insert(entity);
                }
            }

            // 更新感兴趣的标签：先删后插
            if (recommend.getInterestedTags() != null) {
                LambdaQueryWrapper<UserPreferredTag> delWrapper = new LambdaQueryWrapper<>();
                delWrapper.eq(UserPreferredTag::getUserId, userId)
                        .eq(UserPreferredTag::getType, PreferredTypeEnum.INTERESTED);
                userPreferredTagMapper.delete(delWrapper);

                for (Long tagId : recommend.getInterestedTags()) {
                    UserPreferredTag entity = new UserPreferredTag();
                    entity.setUserId(userId);
                    entity.setTagId(tagId);
                    entity.setType(PreferredTypeEnum.INTERESTED);
                    userPreferredTagMapper.insert(entity);
                }
            }

            // 更新屏蔽的标签：先删后插
            if (recommend.getBlockedTags() != null) {
                LambdaQueryWrapper<UserPreferredTag> delWrapper = new LambdaQueryWrapper<>();
                delWrapper.eq(UserPreferredTag::getUserId, userId)
                        .eq(UserPreferredTag::getType, PreferredTypeEnum.BLOCKED);
                userPreferredTagMapper.delete(delWrapper);

                for (Long tagId : recommend.getBlockedTags()) {
                    UserPreferredTag entity = new UserPreferredTag();
                    entity.setUserId(userId);
                    entity.setTagId(tagId);
                    entity.setType(PreferredTypeEnum.BLOCKED);
                    userPreferredTagMapper.insert(entity);
                }
            }
        }
    }

    // ========== 内部方法 ==========

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

        // 解密邮箱和手机号
        try {
            vo.setEmail(user.getEmail() != null ? aesUtil.decrypt(user.getEmail()) : null);
            vo.setPhone(user.getPhone() != null ? aesUtil.decrypt(user.getPhone()) : null);
        } catch (Exception e) {
            log.warn("解密用户敏感信息失败: {}", e.getMessage());
        }

        return vo;
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
        Map<Long, Category> categoryMap = categoryIds.isEmpty() ? Collections.emptyMap()
                : categoryMapper.selectBatchIds(categoryIds).stream()
                .collect(Collectors.toMap(Category::getId, c -> c));

        List<ArticleTag> allArticleTags = articleTagMapper.selectList(
                new LambdaQueryWrapper<ArticleTag>().in(ArticleTag::getArticleId, articleIds));
        Set<Long> tagIds = allArticleTags.stream().map(ArticleTag::getTagId).collect(Collectors.toSet());
        Map<Long, Tag> tagMap = CollectionUtils.isEmpty(tagIds) ? Collections.emptyMap()
                : tagMapper.selectBatchIds(tagIds).stream().collect(Collectors.toMap(Tag::getId, t -> t));
        Map<Long, List<ArticleTag>> articleTagMap = allArticleTags.stream()
                .collect(Collectors.groupingBy(ArticleTag::getArticleId));

        return articles.stream().map(article -> {
            ArticleVO vo = new ArticleVO();
            vo.setId(article.getId());
            vo.setTitle(article.getTitle());
            vo.setSummary(article.getSummary());
            vo.setCoverImage(article.getCoverImage());
            vo.setStatus(article.getStatus() != null ? article.getStatus().getValue() : null);
            // 浏览数 = 数据库值 + Redis增量
            long viewCount = article.getViewCount() != null ? article.getViewCount() : 0L;
            try {
                if (stringRedisTemplate != null) {
                    String viewCountStr = stringRedisTemplate.opsForValue().get(VIEW_COUNT_PREFIX + article.getId());
                    if (viewCountStr != null) {
                        viewCount += Long.parseLong(viewCountStr);
                    }
                }
            } catch (Exception e) {
                log.warn("获取Redis浏览量失败，使用数据库浏览量: {}", e.getMessage());
            }
            vo.setViewCount(viewCount);
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
}
