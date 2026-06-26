package com.zhixun.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhixun.entity.Article;
import com.zhixun.enums.ArticleStatusEnum;
import com.zhixun.mapper.ArticleMapper;
import com.zhixun.mapper.UserMapper;
import com.zhixun.service.CategoryService;
import com.zhixun.service.TagService;
import com.zhixun.common.util.SensitiveWordUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Slf4j
@Component
@ConditionalOnProperty(name = "spring.data.redis.enabled", havingValue = "true", matchIfMissing = true)
@Order(1)
@RequiredArgsConstructor
public class CacheWarmUpRunner implements CommandLineRunner {

    private final CategoryService categoryService;
    private final TagService tagService;
    private final SensitiveWordUtil sensitiveWordUtil;
    private final CacheManager cacheManager;
    private final ArticleMapper articleMapper;
    private final UserMapper userMapper;
    private final CachePenetrationProtection cachePenetrationProtection;

    private static volatile AtomicBoolean CACHE_READY = new AtomicBoolean(false);

    public static boolean isCacheReady() {
        return CACHE_READY.get();
    }

    @Override
    public void run(String... args) {
        log.info("开始缓存预热...");
        long startTime = System.currentTimeMillis();

        try {
            log.info("[同步预热] 开始加载核心元数据缓存...");
            long syncStart = System.currentTimeMillis();
            warmUpCategoryTree();
            warmUpSensitiveWords();
            long syncElapsed = System.currentTimeMillis() - syncStart;
            log.info("[同步预热] 核心元数据缓存加载完成，耗时: {}ms", syncElapsed);

            CACHE_READY.set(true);
            log.info("缓存已就绪，开始接收流量");

            // 异步预热非核心数据
            warmUpHotTags();
            warmUpArticleCache();
            warmUpBloomFilter();
            warmUpHotArticles();
            warmUpActiveUserProfiles();
        } catch (Exception e) {
            log.error("缓存预热异常，不影响应用启动", e);
            CACHE_READY.set(true);
        }

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("缓存预热完成，耗时: {}ms", elapsed);
    }

    public void warmUpCategoryTree() {
        try {
            categoryService.tree();
            log.info("[预热] 分类树缓存加载完成");
        } catch (Exception e) {
            log.warn("[预热] 分类树缓存加载失败: {}", e.getMessage());
        }
    }

    @Async
    public void warmUpHotTags() {
        try {
            tagService.hot(20);
            log.info("[异步预热] 热门标签缓存加载完成");
        } catch (Exception e) {
            log.warn("[异步预热] 热门标签缓存加载失败: {}", e.getMessage());
        }
    }

    public void warmUpSensitiveWords() {
        try {
            sensitiveWordUtil.loadSensitiveWords();
            sensitiveWordUtil.buildDFA();
            log.info("[预热] 敏感词库缓存及 DFA 字典树加载完成");
        } catch (Exception e) {
            log.warn("[预热] 敏感词库缓存加载失败: {}", e.getMessage());
        }
    }

    @Async
    public void warmUpArticleCache() {
        try {
            Cache articleHotCache = cacheManager.getCache("articleHot");
            if (articleHotCache != null) {
                log.info("[异步预热] 热门作品缓存区域已初始化");
            }
            log.info("[异步预热] 热门作品缓存加载完成");
        } catch (Exception e) {
            log.warn("[异步预热] 热门作品缓存加载失败: {}", e.getMessage());
        }
    }

    /**
     * 预热热门作品数据到多级缓存
     * 按浏览量取 Top 50 的已发布作品
     */
    @Async
    public void warmUpHotArticles() {
        try {
            LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Article::getStatus, ArticleStatusEnum.PUBLISHED)
                    .orderByDesc(Article::getViewCount)
                    .last("LIMIT 50");
            List<Article> hotArticles = articleMapper.selectList(wrapper);

            if (!CollectionUtils.isEmpty(hotArticles)) {
                Cache articleHotCache = cacheManager.getCache("articleHot");
                if (articleHotCache != null) {
                    for (Article article : hotArticles) {
                        articleHotCache.put(article.getId(), article);
                    }
                }
                log.info("[异步预热] 热门作品缓存加载完成, count={}", hotArticles.size());
            }
        } catch (Exception e) {
            log.warn("[异步预热] 热门作品缓存加载失败: {}", e.getMessage());
        }
    }

    /**
     * 预热活跃用户资料到多级缓存
     * 取最近注册的 100 个用户
     */
    @Async
    public void warmUpActiveUserProfiles() {
        try {
            Cache userProfileCache = cacheManager.getCache("userProfile");
            if (userProfileCache == null) {
                return;
            }

            // 查询最近活跃的用户（按注册时间倒序取100个）
            List<com.zhixun.entity.User> activeUsers = userMapper.selectList(
                    new LambdaQueryWrapper<com.zhixun.entity.User>()
                            .orderByDesc(com.zhixun.entity.User::getCreatedAt)
                            .last("LIMIT 100"));

            if (!CollectionUtils.isEmpty(activeUsers)) {
                for (com.zhixun.entity.User user : activeUsers) {
                    userProfileCache.put(user.getId(), user);
                }
                log.info("[异步预热] 活跃用户资料缓存加载完成, count={}", activeUsers.size());
            }
        } catch (Exception e) {
            log.warn("[异步预热] 活跃用户资料缓存加载失败: {}", e.getMessage());
        }
    }

    /**
     * 初始化布隆过滤器
     * 将所有已发布作品的 ID 加入布隆过滤器，防止缓存穿透
     */
    @Async
    public void warmUpBloomFilter() {
        try {
            if (cachePenetrationProtection.isBloomInitialized()) {
                log.info("[异步预热] 布隆过滤器已初始化，跳过");
                return;
            }

            // 分批加载作品 ID，避免一次性加载过多数据
            long offset = 0;
            int batchSize = 1000;
            int totalCount = 0;

            while (true) {
                List<Article> articles = articleMapper.selectList(
                        new LambdaQueryWrapper<Article>()
                                .eq(Article::getStatus, ArticleStatusEnum.PUBLISHED)
                                .select(Article::getId)
                                .last("LIMIT " + batchSize + " OFFSET " + offset));

                if (CollectionUtils.isEmpty(articles)) {
                    break;
                }

                List<String> articleIds = articles.stream()
                        .map(a -> String.valueOf(a.getId()))
                        .collect(Collectors.toList());
                cachePenetrationProtection.bloomAddAll(articleIds);
                totalCount += articles.size();
                offset += batchSize;

                if (articles.size() < batchSize) {
                    break;
                }
            }

            cachePenetrationProtection.markBloomInitialized();
            log.info("[异步预热] 布隆过滤器初始化完成, 作品数={}", totalCount);
        } catch (Exception e) {
            log.warn("[异步预热] 布隆过滤器初始化失败: {}", e.getMessage());
        }
    }
}
