package com.zhixun.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhixun.common.result.PageResult;
import com.zhixun.config.Slave;
import com.zhixun.entity.Article;
import com.zhixun.entity.User;
import com.zhixun.enums.ArticleStatusEnum;
import com.zhixun.mapper.ArticleMapper;
import com.zhixun.mapper.UserMapper;
import com.zhixun.vo.ArticleVO;
import com.zhixun.vo.HotArticleVO;
import com.zhixun.vo.SearchResultVO;
import com.zhixun.vo.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 降级策略服务
 * 当主服务不可用或触发熔断时，提供降级处理逻辑
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FallbackService {

    private final ArticleMapper articleMapper;
    private final UserMapper userMapper;
    private final StringRedisTemplate stringRedisTemplate;

    /** 热门作品降级缓存 Key */
    private static final String FALLBACK_HOT_ARTICLES_KEY = "fallback:hot_articles";
    /** 降级缓存 TTL（10分钟） */
    private static final long FALLBACK_CACHE_TTL_MINUTES = 10;
    /** 排行榜降级缓存 Key 前缀 */
    private static final String FALLBACK_RANK_KEY_PREFIX = "fallback:rank:";

    /**
     * 推荐服务降级：返回热门作品列表
     * 当推荐引擎（协同过滤/Bandit/内容推荐）不可用时使用
     *
     * @param page     页码
     * @param pageSize 每页大小
     * @return 热门作品分页结果
     */
    @Slave
    public PageResult<ArticleVO> getRecommendFallback(Integer page, Integer pageSize) {
        log.warn("推荐服务降级：返回热门作品列表, page={}, pageSize={}", page, pageSize);

        // 尝试从降级缓存获取
        String cacheKey = FALLBACK_HOT_ARTICLES_KEY + ":page:" + page + ":size:" + pageSize;
        String cachedIds = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cachedIds != null && !cachedIds.isEmpty()) {
            List<Long> articleIds = parseIds(cachedIds);
            if (!articleIds.isEmpty()) {
                List<Article> articles = articleMapper.selectBatchIds(articleIds);
                List<ArticleVO> voList = convertToSimpleVOList(articles);
                return new PageResult<>(voList, (long) articleIds.size(), page, pageSize);
            }
        }

        // 缓存未命中，查询数据库
        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Article::getStatus, ArticleStatusEnum.PUBLISHED)
                .orderByDesc(Article::getViewCount)
                .orderByDesc(Article::getLikeCount)
                .orderByDesc(Article::getCreatedAt);

        List<Article> allArticles = articleMapper.selectList(wrapper);

        // 分页
        int fromIndex = (page - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, allArticles.size());

        if (fromIndex >= allArticles.size()) {
            return new PageResult<>(Collections.emptyList(), (long) allArticles.size(), page, pageSize);
        }

        List<Article> pagedArticles = allArticles.subList(fromIndex, toIndex);
        List<ArticleVO> voList = convertToSimpleVOList(pagedArticles);

        // 缓存降级结果
        if (!voList.isEmpty()) {
            String idsStr = pagedArticles.stream()
                    .map(a -> String.valueOf(a.getId()))
                    .collect(Collectors.joining(","));
            stringRedisTemplate.opsForValue().set(cacheKey, idsStr, FALLBACK_CACHE_TTL_MINUTES, TimeUnit.MINUTES);
        }

        return new PageResult<>(voList, (long) allArticles.size(), page, pageSize);
    }

    /**
     * 搜索服务降级：使用数据库 LIKE 查询
     * 当 OpenSearch 不可用或搜索熔断时使用
     *
     * @param keyword  搜索关键词
     * @param page     页码
     * @param pageSize 每页大小
     * @return 搜索结果
     */
    @Slave
    public SearchResultVO getSearchFallback(String keyword, Integer page, Integer pageSize) {
        log.warn("搜索服务降级：使用数据库 LIKE 查询, keyword={}", keyword);

        SearchResultVO result = new SearchResultVO();
        result.setKeyword(keyword);
        result.setType("article");

        if (keyword == null || keyword.trim().isEmpty()) {
            result.setArticles(Collections.emptyList());
            result.setTotal(0L);
            return result;
        }

        // 使用数据库模糊搜索
        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(Article::getTitle, keyword)
                .or()
                .like(Article::getSummary, keyword)
                .or()
                .like(Article::getContent, keyword);
        wrapper.eq(Article::getStatus, ArticleStatusEnum.PUBLISHED);

        long total = articleMapper.selectCount(wrapper);
        // 使用 MyBatis-Plus 分页替代 SQL 拼接，避免 .last() 注入隐患
        Page<Article> articlePage = new Page<>(page, pageSize);
        List<Article> articles = articleMapper.selectPage(articlePage, wrapper).getRecords();

        List<ArticleVO> voList = articles.stream().map(a -> {
            ArticleVO vo = new ArticleVO();
            vo.setId(a.getId());
            vo.setTitle(a.getTitle());
            vo.setSummary(a.getSummary());
            vo.setCoverImage(a.getCoverImage());
            vo.setViewCount(a.getViewCount());
            vo.setLikeCount(a.getLikeCount());
            vo.setCommentCount(a.getCommentCount());
            vo.setCreatedAt(a.getCreatedAt());
            return vo;
        }).collect(Collectors.toList());

        result.setArticles(voList);
        result.setTotal(total);
        return result;
    }

    /**
     * 用户搜索服务降级：使用数据库 LIKE 查询
     * 当 OpenSearch 用户搜索不可用时使用
     *
     * @param keyword  搜索关键词
     * @param page     页码
     * @param pageSize 每页大小
     * @return 搜索结果（仅包含用户列表）
     */
    @Slave
    public SearchResultVO getUserSearchFallback(String keyword, Integer page, Integer pageSize) {
        log.warn("用户搜索服务降级：使用数据库 LIKE 查询, keyword={}", keyword);

        SearchResultVO result = new SearchResultVO();
        result.setKeyword(keyword);
        result.setType("user");

        if (keyword == null || keyword.trim().isEmpty()) {
            result.setUsers(Collections.emptyList());
            result.setTotal(0L);
            return result;
        }

        // 多字段模糊搜索：昵称、用户名、UID
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w
                .like(User::getNickname, keyword)
                .or().like(User::getUsername, keyword)
                .or().like(User::getUid, keyword)
        );

        long total = userMapper.selectCount(wrapper);

        // MyBatis-Plus 分页
        Page<User> userPage = new Page<>(page, pageSize);
        List<User> users = userMapper.selectPage(userPage, wrapper).getRecords();

        List<UserVO> userVOs = users.stream().map(u -> {
            UserVO vo = new UserVO();
            vo.setId(u.getId());
            vo.setUid(u.getUid());
            vo.setUsername(u.getUsername());
            vo.setNickname(u.getNickname());
            vo.setAvatar(u.getAvatar());
            vo.setBio(u.getBio());
            vo.setArticleCount(u.getArticleCount());
            vo.setFollowerCount(u.getFollowerCount());
            vo.setCreatedAt(u.getCreatedAt());
            return vo;
        }).collect(Collectors.toList());

        result.setUsers(userVOs);
        result.setTotal(total);
        return result;
    }

    /**
     * 排行榜降级：返回缓存数据
     * 当实时排行榜计算不可用时使用
     *
     * @param period     时间周期
     * @param categoryId 分类ID
     * @param limit      返回数量
     * @return 热门作品列表
     */
    public List<HotArticleVO> getRankFallback(String period, Long categoryId, Integer limit) {
        log.warn("排行榜降级：返回缓存数据, period={}, categoryId={}", period, categoryId);

        if (limit == null || limit <= 0) {
            limit = 20;
        }

        // 尝试从降级缓存获取
        String cacheKey = FALLBACK_RANK_KEY_PREFIX + period + ":" + (categoryId != null ? categoryId : "all");
        String cachedIds = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cachedIds != null && !cachedIds.isEmpty()) {
            List<Long> articleIds = parseIds(cachedIds);
            if (!articleIds.isEmpty()) {
                List<Long> pageIds = articleIds.stream().limit(limit).collect(Collectors.toList());
                List<Article> articles = articleMapper.selectBatchIds(pageIds);
                return articles.stream().map(a -> {
                    HotArticleVO vo = new HotArticleVO();
                    vo.setId(a.getId());
                    vo.setTitle(a.getTitle());
                    vo.setViewCount(a.getViewCount() != null ? a.getViewCount().intValue() : 0);
                    vo.setLikeCount(a.getLikeCount() != null ? a.getLikeCount().intValue() : 0);
                    vo.setCommentCount(a.getCommentCount() != null ? a.getCommentCount().intValue() : 0);
                    vo.setCreatedAt(a.getCreatedAt());
                    return vo;
                }).collect(Collectors.toList());
            }
        }

        // 缓存也没有，返回空列表（排行榜降级不查数据库，避免雪崩）
        log.warn("排行榜降级缓存也为空，返回空列表");
        return Collections.emptyList();
    }

    // ========== 内部方法 ==========

    /**
     * 将作品列表转换为简单 VO 列表
     */
    private List<ArticleVO> convertToSimpleVOList(List<Article> articles) {
        if (CollectionUtils.isEmpty(articles)) {
            return Collections.emptyList();
        }
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
            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 解析缓存的ID字符串
     */
    private List<Long> parseIds(String idsStr) {
        String[] parts = idsStr.split(",");
        return java.util.Arrays.stream(parts)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> {
                    try {
                        return Long.parseLong(s);
                    } catch (NumberFormatException e) {
                        return null;
                    }
                })
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
    }
}
