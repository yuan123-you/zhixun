package com.zhixun.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhixun.config.OpenSearchConfig;
import com.zhixun.entity.Article;
import com.zhixun.entity.ArticleImage;
import com.zhixun.entity.ArticleTag;
import com.zhixun.entity.Category;
import com.zhixun.entity.Tag;
import com.zhixun.entity.User;
import com.zhixun.enums.ArticleStatusEnum;
import com.zhixun.mapper.ArticleImageMapper;
import com.zhixun.mapper.ArticleMapper;
import com.zhixun.mapper.ArticleTagMapper;
import com.zhixun.mapper.CategoryMapper;
import com.zhixun.mapper.TagMapper;
import com.zhixun.mapper.UserMapper;
import com.zhixun.service.OpenSearchSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.core.BulkRequest;
import org.opensearch.client.opensearch.core.BulkResponse;
import org.opensearch.client.opensearch.core.IndexRequest;
import org.opensearch.client.opensearch.core.bulk.BulkResponseItem;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * OpenSearch 数据同步服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnBean(OpenSearchClient.class)
public class OpenSearchSyncServiceImpl implements OpenSearchSyncService {

    private final OpenSearchClient openSearchClient;
    private final OpenSearchConfig openSearchConfig;
    private final ArticleMapper articleMapper;
    private final UserMapper userMapper;
    private final CategoryMapper categoryMapper;
    private final TagMapper tagMapper;
    private final ArticleTagMapper articleTagMapper;
    private final ArticleImageMapper articleImageMapper;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int BULK_SIZE = 500;

    // ========== 文章同步 ==========

    @Override
    @Async
    public void syncArticle(Long articleId) {
        try {
            Article article = articleMapper.selectById(articleId);
            if (article == null || article.getStatus() != ArticleStatusEnum.PUBLISHED) {
                deleteArticle(articleId);
                return;
            }
            Map<String, Object> doc = buildArticleDoc(article);
            IndexRequest<Map<String, Object>> request = IndexRequest.of(b -> b
                    .index(openSearchConfig.getArticleIndex())
                    .id(String.valueOf(articleId))
                    .document(doc)
            );
            openSearchClient.index(request);
            log.debug("同步文章到 OpenSearch: articleId={}", articleId);
        } catch (Exception e) {
            log.error("同步文章到 OpenSearch 失败: articleId={}, error={}", articleId, e.getMessage());
        }
    }

    @Override
    @Async
    public void syncArticles(List<Long> articleIds) {
        if (CollectionUtils.isEmpty(articleIds)) return;
        try {
            List<Article> articles = articleMapper.selectBatchIds(articleIds);
            if (CollectionUtils.isEmpty(articles)) return;

            BulkRequest.Builder bulkBuilder = new BulkRequest.Builder();
            for (Article article : articles) {
                if (article.getStatus() != ArticleStatusEnum.PUBLISHED) continue;
                Map<String, Object> doc = buildArticleDoc(article);
                bulkBuilder.operations(op -> op
                        .index(idx -> idx
                                .index(openSearchConfig.getArticleIndex())
                                .id(String.valueOf(article.getId()))
                                .document(doc)
                        )
                );
            }
            BulkResponse response = openSearchClient.bulk(bulkBuilder.build());
            if (response.errors()) {
                for (BulkResponseItem item : response.items()) {
                    if (item.error() != null) {
                        log.error("批量同步文章失败: id={}, error={}", item.id(), item.error().reason());
                    }
                }
            } else {
                log.debug("批量同步文章到 OpenSearch: count={}", articles.size());
            }
        } catch (Exception e) {
            log.error("批量同步文章到 OpenSearch 失败: error={}", e.getMessage());
        }
    }

    @Override
    public void deleteArticle(Long articleId) {
        try {
            openSearchClient.delete(d -> d
                    .index(openSearchConfig.getArticleIndex())
                    .id(String.valueOf(articleId))
            );
            log.debug("从 OpenSearch 删除文章: articleId={}", articleId);
        } catch (IOException e) {
            log.error("从 OpenSearch 删除文章失败: articleId={}, error={}", articleId, e.getMessage());
        }
    }

    // ========== 用户同步 ==========

    @Override
    @Async
    public void syncUser(Long userId) {
        try {
            User user = userMapper.selectById(userId);
            if (user == null) {
                deleteUser(userId);
                return;
            }
            Map<String, Object> doc = buildUserDoc(user);
            IndexRequest<Map<String, Object>> request = IndexRequest.of(b -> b
                    .index(openSearchConfig.getUserIndex())
                    .id(String.valueOf(userId))
                    .document(doc)
            );
            openSearchClient.index(request);
            log.debug("同步用户到 OpenSearch: userId={}", userId);
        } catch (Exception e) {
            log.error("同步用户到 OpenSearch 失败: userId={}, error={}", userId, e.getMessage());
        }
    }

    @Override
    @Async
    public void syncUsers(List<Long> userIds) {
        if (CollectionUtils.isEmpty(userIds)) return;
        try {
            List<User> users = userMapper.selectBatchIds(userIds);
            if (CollectionUtils.isEmpty(users)) return;

            BulkRequest.Builder bulkBuilder = new BulkRequest.Builder();
            for (User user : users) {
                Map<String, Object> doc = buildUserDoc(user);
                bulkBuilder.operations(op -> op
                        .index(idx -> idx
                                .index(openSearchConfig.getUserIndex())
                                .id(String.valueOf(user.getId()))
                                .document(doc)
                        )
                );
            }
            openSearchClient.bulk(bulkBuilder.build());
            log.debug("批量同步用户到 OpenSearch: count={}", users.size());
        } catch (Exception e) {
            log.error("批量同步用户到 OpenSearch 失败: error={}", e.getMessage());
        }
    }

    @Override
    public void deleteUser(Long userId) {
        try {
            openSearchClient.delete(d -> d
                    .index(openSearchConfig.getUserIndex())
                    .id(String.valueOf(userId))
            );
            log.debug("从 OpenSearch 删除用户: userId={}", userId);
        } catch (IOException e) {
            log.error("从 OpenSearch 删除用户失败: userId={}, error={}", userId, e.getMessage());
        }
    }

    // ========== 图片同步 ==========

    @Override
    @Async
    public void syncImage(Long imageId) {
        try {
            ArticleImage image = articleImageMapper.selectById(imageId);
            if (image == null) {
                deleteImage(imageId);
                return;
            }
            Map<String, Object> doc = buildImageDoc(image);
            IndexRequest<Map<String, Object>> request = IndexRequest.of(b -> b
                    .index(openSearchConfig.getImageIndex())
                    .id(String.valueOf(imageId))
                    .document(doc)
            );
            openSearchClient.index(request);
            log.debug("同步图片到 OpenSearch: imageId={}", imageId);
        } catch (Exception e) {
            log.error("同步图片到 OpenSearch 失败: imageId={}, error={}", imageId, e.getMessage());
        }
    }

    @Override
    @Async
    public void syncImagesByArticle(Long articleId) {
        try {
            List<ArticleImage> images = articleImageMapper.selectList(
                    new LambdaQueryWrapper<ArticleImage>().eq(ArticleImage::getArticleId, articleId));
            if (CollectionUtils.isEmpty(images)) return;

            BulkRequest.Builder bulkBuilder = new BulkRequest.Builder();
            for (ArticleImage image : images) {
                Map<String, Object> doc = buildImageDoc(image);
                bulkBuilder.operations(op -> op
                        .index(idx -> idx
                                .index(openSearchConfig.getImageIndex())
                                .id(String.valueOf(image.getId()))
                                .document(doc)
                        )
                );
            }
            openSearchClient.bulk(bulkBuilder.build());
            log.debug("同步文章图片到 OpenSearch: articleId={}, count={}", articleId, images.size());
        } catch (Exception e) {
            log.error("同步文章图片到 OpenSearch 失败: articleId={}, error={}", articleId, e.getMessage());
        }
    }

    @Override
    public void deleteImage(Long imageId) {
        try {
            openSearchClient.delete(d -> d
                    .index(openSearchConfig.getImageIndex())
                    .id(String.valueOf(imageId))
            );
        } catch (IOException e) {
            log.error("从 OpenSearch 删除图片失败: imageId={}, error={}", imageId, e.getMessage());
        }
    }

    @Override
    public void deleteImagesByArticle(Long articleId) {
        try {
            openSearchClient.deleteByQuery(d -> d
                    .index(openSearchConfig.getImageIndex())
                    .query(q -> q
                            .term(t -> t
                                    .field("articleId")
                                    .value(FieldValue.of(articleId))
                            )
                    )
            );
        } catch (IOException e) {
            log.error("从 OpenSearch 删除文章图片失败: articleId={}, error={}", articleId, e.getMessage());
        }
    }

    // ========== 全量同步 ==========

    @Override
    public void fullSync() {
        log.info("开始全量同步到 OpenSearch...");
        fullSyncArticles();
        fullSyncUsers();
        fullSyncImages();
        log.info("全量同步到 OpenSearch 完成");
    }

    @Override
    public void fullSyncArticles() {
        log.info("开始全量同步文章...");
        try {
            long total = articleMapper.selectCount(
                    new LambdaQueryWrapper<Article>().eq(Article::getStatus, ArticleStatusEnum.PUBLISHED));
            log.info("待同步文章总数: {}", total);

            long processed = 0;
            long lastId = 0;
            while (processed < total) {
                List<Article> articles = articleMapper.selectList(
                        new LambdaQueryWrapper<Article>()
                                .eq(Article::getStatus, ArticleStatusEnum.PUBLISHED)
                                .gt(Article::getId, lastId)
                                .orderByAsc(Article::getId)
                                .last("LIMIT " + BULK_SIZE));

                if (CollectionUtils.isEmpty(articles)) break;

                BulkRequest.Builder bulkBuilder = new BulkRequest.Builder();
                for (Article article : articles) {
                    Map<String, Object> doc = buildArticleDoc(article);
                    bulkBuilder.operations(op -> op
                            .index(idx -> idx
                                    .index(openSearchConfig.getArticleIndex())
                                    .id(String.valueOf(article.getId()))
                                    .document(doc)
                            )
                    );
                }
                openSearchClient.bulk(bulkBuilder.build());

                processed += articles.size();
                lastId = articles.get(articles.size() - 1).getId();
                log.info("文章同步进度: {}/{}", processed, total);
            }
        } catch (Exception e) {
            log.error("全量同步文章失败: {}", e.getMessage());
        }
    }

    @Override
    public void fullSyncUsers() {
        log.info("开始全量同步用户...");
        try {
            long total = userMapper.selectCount(null);
            log.info("待同步用户总数: {}", total);

            long processed = 0;
            long lastId = 0;
            while (processed < total) {
                List<User> users = userMapper.selectList(
                        new LambdaQueryWrapper<User>()
                                .gt(User::getId, lastId)
                                .orderByAsc(User::getId)
                                .last("LIMIT " + BULK_SIZE));

                if (CollectionUtils.isEmpty(users)) break;

                BulkRequest.Builder bulkBuilder = new BulkRequest.Builder();
                for (User user : users) {
                    Map<String, Object> doc = buildUserDoc(user);
                    bulkBuilder.operations(op -> op
                            .index(idx -> idx
                                    .index(openSearchConfig.getUserIndex())
                                    .id(String.valueOf(user.getId()))
                                    .document(doc)
                            )
                    );
                }
                openSearchClient.bulk(bulkBuilder.build());

                processed += users.size();
                lastId = users.get(users.size() - 1).getId();
                log.info("用户同步进度: {}/{}", processed, total);
            }
        } catch (Exception e) {
            log.error("全量同步用户失败: {}", e.getMessage());
        }
    }

    @Override
    public void fullSyncImages() {
        log.info("开始全量同步图片...");
        try {
            // 只同步已发布文章的图片
            List<Article> publishedArticles = articleMapper.selectList(
                    new LambdaQueryWrapper<Article>()
                            .eq(Article::getStatus, ArticleStatusEnum.PUBLISHED)
                            .select(Article::getId));

            if (CollectionUtils.isEmpty(publishedArticles)) return;

            List<Long> articleIds = publishedArticles.stream().map(Article::getId).collect(Collectors.toList());
            List<ArticleImage> images = articleImageMapper.selectList(
                    new LambdaQueryWrapper<ArticleImage>().in(ArticleImage::getArticleId, articleIds));

            log.info("待同步图片总数: {}", images.size());

            // 构建文章信息缓存
            Map<Long, Article> articleMap = publishedArticles.stream()
                    .collect(Collectors.toMap(Article::getId, a -> a));

            int processed = 0;
            while (processed < images.size()) {
                int end = Math.min(processed + BULK_SIZE, images.size());
                List<ArticleImage> batch = images.subList(processed, end);

                BulkRequest.Builder bulkBuilder = new BulkRequest.Builder();
                for (ArticleImage image : batch) {
                    Map<String, Object> doc = buildImageDoc(image, articleMap.get(image.getArticleId()));
                    bulkBuilder.operations(op -> op
                            .index(idx -> idx
                                    .index(openSearchConfig.getImageIndex())
                                    .id(String.valueOf(image.getId()))
                                    .document(doc)
                            )
                    );
                }
                openSearchClient.bulk(bulkBuilder.build());

                processed = end;
                log.info("图片同步进度: {}/{}", processed, images.size());
            }
        } catch (Exception e) {
            log.error("全量同步图片失败: {}", e.getMessage());
        }
    }

    // ========== 文档构建方法 ==========

    private Map<String, Object> buildArticleDoc(Article article) {
        // 查询作者
        User author = userMapper.selectById(article.getAuthorId());
        String authorName = author != null ? author.getNickname() : null;
        String authorAvatar = author != null ? author.getAvatar() : null;

        // 查询分类
        String categoryName = null;
        if (article.getCategoryId() != null) {
            Category category = categoryMapper.selectById(article.getCategoryId());
            categoryName = category != null ? category.getName() : null;
        }

        // 查询标签
        List<ArticleTag> articleTags = articleTagMapper.selectList(
                new LambdaQueryWrapper<ArticleTag>().eq(ArticleTag::getArticleId, article.getId()));
        List<Map<String, Object>> tags = Collections.emptyList();
        if (!CollectionUtils.isEmpty(articleTags)) {
            Set<Long> tagIds = articleTags.stream().map(ArticleTag::getTagId).collect(Collectors.toSet());
            List<Tag> tagList = tagMapper.selectBatchIds(tagIds);
            tags = tagList.stream().map(t -> {
                Map<String, Object> tagMap = new java.util.HashMap<>();
                tagMap.put("id", t.getId());
                tagMap.put("name", t.getName());
                return tagMap;
            }).collect(Collectors.toList());
        }

        Map<String, Object> doc = new java.util.LinkedHashMap<>();
        doc.put("id", article.getId());
        doc.put("authorId", article.getAuthorId());
        doc.put("categoryId", article.getCategoryId());
        doc.put("title", article.getTitle());
        doc.put("summary", article.getSummary());
        doc.put("content", article.getContent());
        doc.put("coverImage", article.getCoverImage());
        doc.put("status", article.getStatus() != null ? article.getStatus().getValue() : null);
        doc.put("isTop", article.getIsTop());
        doc.put("isRecommend", article.getIsRecommend());
        doc.put("viewCount", article.getViewCount());
        doc.put("likeCount", article.getLikeCount());
        doc.put("commentCount", article.getCommentCount());
        doc.put("collectCount", article.getCollectCount());
        doc.put("hotScore", article.getHotScore());
        doc.put("authorName", authorName);
        doc.put("authorAvatar", authorAvatar);
        doc.put("categoryName", categoryName);
        doc.put("tags", tags);
        doc.put("publishAt", article.getPublishAt() != null ? article.getPublishAt().format(DATE_FORMATTER) : null);
        doc.put("createdAt", article.getCreatedAt() != null ? article.getCreatedAt().format(DATE_FORMATTER) : null);
        doc.put("updatedAt", article.getUpdatedAt() != null ? article.getUpdatedAt().format(DATE_FORMATTER) : null);
        return doc;
    }

    private Map<String, Object> buildUserDoc(User user) {
        Map<String, Object> doc = new java.util.LinkedHashMap<>();
        doc.put("id", user.getId());
        doc.put("uid", user.getUid());
        doc.put("username", user.getUsername());
        doc.put("nickname", user.getNickname());
        doc.put("avatar", user.getAvatar());
        doc.put("bio", user.getBio());
        doc.put("province", user.getProvince());
        doc.put("ipLocation", user.getIpLocation());
        doc.put("role", user.getRole() != null ? user.getRole().getValue() : null);
        doc.put("status", user.getStatus());
        doc.put("followCount", user.getFollowCount());
        doc.put("followerCount", user.getFollowerCount());
        doc.put("articleCount", user.getArticleCount());
        doc.put("createdAt", user.getCreatedAt() != null ? user.getCreatedAt().format(DATE_FORMATTER) : null);
        return doc;
    }

    private Map<String, Object> buildImageDoc(ArticleImage image) {
        Article article = articleMapper.selectById(image.getArticleId());
        return buildImageDoc(image, article);
    }

    private Map<String, Object> buildImageDoc(ArticleImage image, Article article) {
        Map<String, Object> doc = new java.util.LinkedHashMap<>();
        doc.put("id", image.getId());
        doc.put("articleId", image.getArticleId());
        doc.put("url", image.getUrl());
        doc.put("articleTitle", article != null ? article.getTitle() : null);
        doc.put("articleCoverImage", article != null ? article.getCoverImage() : null);
        doc.put("articleCreatedAt", article != null && article.getCreatedAt() != null
                ? article.getCreatedAt().format(DATE_FORMATTER) : null);
        doc.put("createdAt", image.getCreatedAt() != null ? image.getCreatedAt().format(DATE_FORMATTER) : null);
        return doc;
    }
}
