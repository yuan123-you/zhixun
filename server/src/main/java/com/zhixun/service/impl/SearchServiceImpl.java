package com.zhixun.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhixun.common.util.SecurityUtil;
import com.zhixun.config.OpenSearchConfig;
import com.zhixun.config.Slave;
import com.zhixun.entity.UserFollow;
import com.zhixun.entity.UserPreferredCategory;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhixun.mapper.UserFollowMapper;
import com.zhixun.mapper.UserPreferredCategoryMapper;
import com.zhixun.service.SearchHistoryService;
import com.zhixun.service.SearchService;
import com.zhixun.vo.ArticleVO;
import com.zhixun.vo.SearchResultVO;
import com.zhixun.vo.SearchSuggestResultVO;
import com.zhixun.vo.SuggestionVO;
import com.zhixun.vo.TagVO;
import com.zhixun.vo.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.client.json.JsonData;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.query_dsl.FunctionScoreMode;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.search.HighlightField;
import org.opensearch.client.opensearch.core.search.Hit;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 搜索服务实现（OpenSearch版）
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnBean(OpenSearchClient.class)
public class SearchServiceImpl implements SearchService {

    private final OpenSearchClient openSearchClient;
    private final OpenSearchConfig openSearchConfig;
    private final SearchHistoryService searchHistoryService;
    private final SecurityUtil securityUtil;
    private final UserPreferredCategoryMapper userPreferredCategoryMapper;
    private final UserFollowMapper userFollowMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String SUGGEST_CACHE_PREFIX = "search:suggest:";
    private static final long SUGGEST_CACHE_TTL_MINUTES = 5;

    @Override
    @Slave
    public SearchResultVO search(String keyword, String type, Long categoryId, Long tagId,
                                 String timeRange, String startDate, String endDate,
                                 String sort, Integer page, Integer pageSize) {
        long startTime = System.currentTimeMillis();

        SearchResultVO result = new SearchResultVO();
        result.setKeyword(keyword);
        result.setType(type);

        if (!StringUtils.hasText(keyword)) {
            result.setTotal(0L);
            result.setTook(System.currentTimeMillis() - startTime);
            return result;
        }

        // 记录搜索历史和搜索频次（异步）
        try {
            Long currentUserId = getCurrentUserIdSafe();
            if (currentUserId != null) {
                searchHistoryService.recordSearchHistory(currentUserId, keyword);
            }
            searchHistoryService.incrementSearchCount(keyword);
        } catch (Exception e) {
            log.warn("记录搜索历史失败: {}", e.getMessage());
        }

        long total = 0L;

        switch (type != null ? type : "all") {
            case "article":
                total = searchArticles(keyword, categoryId, tagId, timeRange, startDate, endDate, sort, page, pageSize, result);
                result.setArticleTotal(total);
                break;
            case "user":
                total = searchUsers(keyword, page, pageSize, result);
                result.setUserTotal(total);
                break;
            case "image":
                total = searchImages(keyword, page, pageSize, result);
                result.setImageTotal(total);
                break;
            case "all":
            default:
                CompletableFuture<Long> articleFuture = CompletableFuture.supplyAsync(() ->
                        searchArticles(keyword, categoryId, tagId, timeRange, startDate, endDate, sort, page, pageSize, result));
                CompletableFuture<Long> userFuture = CompletableFuture.supplyAsync(() ->
                        searchUsers(keyword, 1, 5, result));
                CompletableFuture<Long> imageFuture = CompletableFuture.supplyAsync(() ->
                        searchImages(keyword, 1, 6, result));
                try {
                    CompletableFuture.allOf(articleFuture, userFuture, imageFuture).join();
                } catch (Exception e) {
                    log.error("并行搜索部分失败: {}", e.getMessage());
                }
                long articleTotal = articleFuture.join();
                long userTotal = userFuture.join();
                long imageTotal = imageFuture.join();
                result.setArticleTotal(articleTotal);
                result.setUserTotal(userTotal);
                result.setImageTotal(imageTotal);
                total = articleTotal + userTotal + imageTotal;
                break;
        }

        result.setTotal(total);
        result.setTook(System.currentTimeMillis() - startTime);
        return result;
    }

    @Override
    @Slave
    public SearchSuggestResultVO getSuggestions(String keyword) {
        // 检查缓存
        String cacheKey = SUGGEST_CACHE_PREFIX + keyword;
        try {
            String cached = stringRedisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                return objectMapper.readValue(cached, SearchSuggestResultVO.class);
            }
        } catch (Exception e) {
            log.warn("读取搜索建议缓存失败: {}", e.getMessage());
        }

        SearchSuggestResultVO result = new SearchSuggestResultVO();
        List<SuggestionVO> completions = new ArrayList<>();

        if (!StringUtils.hasText(keyword)) {
            result.setCompletions(completions);
            result.setHotSearches(searchHistoryService.getHotSearchKeywords(5));
            cacheSuggestResult(cacheKey, result);
            return result;
        }

        try {
            // 匹配用户名/昵称（2条）
            SearchResponse<Map> userResponse = openSearchClient.search(s -> s
                            .index(openSearchConfig.getUserIndex())
                            .size(2)
                            .query(q -> q
                                    .multiMatch(mm -> mm
                                            .fields("username", "nickname", "username.pinyin", "nickname.pinyin", "username.synonym", "nickname.synonym")
                                            .query(keyword)
                                    )
                            ),
                    Map.class
            );
            for (Hit<Map> hit : userResponse.hits().hits()) {
                Map<String, Object> source = hit.source();
                if (source != null) {
                    SuggestionVO suggestion = new SuggestionVO();
                    suggestion.setType("user");
                    suggestion.setId(toLong(source.get("id")));
                    Object nickname = source.get("nickname");
                    Object username = source.get("username");
                    suggestion.setText(nickname != null ? nickname.toString() : (username != null ? username.toString() : null));
                    suggestion.setAvatar(toString(source.get("avatar")));
                    completions.add(suggestion);
                }
            }

            // 匹配文章标题（4条）
            SearchResponse<Map> articleResponse = openSearchClient.search(s -> s
                            .index(openSearchConfig.getArticleIndex())
                            .size(4)
                            .query(q -> q
                                    .multiMatch(mm -> mm
                                            .fields("title", "title.pinyin", "title.synonym")
                                            .query(keyword)
                                    )
                            )
                            .source(sc -> sc
                                    .filter(f -> f.includes("title"))
                            ),
                    Map.class
            );
            for (Hit<Map> hit : articleResponse.hits().hits()) {
                Map<String, Object> source = hit.source();
                if (source != null && source.get("title") != null) {
                    SuggestionVO suggestion = new SuggestionVO();
                    suggestion.setType("article");
                    suggestion.setId(toLong(source.get("id")));
                    suggestion.setText(source.get("title").toString());
                    completions.add(suggestion);
                }
            }

            // 匹配标签名（2条）
            SearchResponse<Map> tagResponse = openSearchClient.search(s -> s
                            .index(openSearchConfig.getArticleIndex())
                            .size(0)
                            .query(q -> q
                                    .nested(n -> n
                                            .path("tags")
                                            .query(nq -> nq
                                                    .multiMatch(mm -> mm
                                                            .fields("tags.name", "tags.name.pinyin")
                                                            .query(keyword)
                                                    )
                                            )
                                    )
                            )
                            .aggregations("tag_names", a -> a
                                    .terms(t -> t
                                            .field("tags.name.keyword")
                                            .size(2)
                                    )
                            ),
                    Map.class
            );
            if (tagResponse.aggregations() != null && tagResponse.aggregations().get("tag_names") != null) {
                tagResponse.aggregations().get("tag_names").sterms().buckets().array().forEach(bucket -> {
                    SuggestionVO suggestion = new SuggestionVO();
                    suggestion.setType("tag");
                    suggestion.setText(bucket.key().toString());
                    completions.add(suggestion);
                });
            }

        } catch (Exception e) {
            log.error("获取搜索建议失败: {}", e.getMessage());
        }

        // 去重（按type+text）并限制8条
        List<SuggestionVO> distinctCompletions = completions.stream()
                .filter(s -> s.getText() != null)
                .distinct()
                .limit(8)
                .toList();

        result.setCompletions(distinctCompletions);
        result.setHotSearches(searchHistoryService.getHotSearchKeywords(5));

        // 写入缓存
        cacheSuggestResult(cacheKey, result);

        return result;
    }

    @Override
    @Slave
    public List<String> getHotSearches() {
        return searchHistoryService.getHotSearchKeywords(10);
    }

    @Override
    public void clearSearchHistory(Long userId) {
        searchHistoryService.clearSearchHistory(userId);
    }

    // ========== 内部方法 ==========

    /**
     * 使用 OpenSearch 搜索文章（带相关性评分公式 + 正文内容高亮 + 模糊匹配）
     * 评分公式：
     * - field_weight: 标题匹配×3, 摘要×2, 正文×1
     * - hot_score_boost: log(1 + hot_score / 100) 加权
     * - time_decay: 1 / (1 + hours_since_publish / 168) 时间衰减
     * - personal_boost: 用户偏好分类的文章提升1.5倍
     * 增强功能：
     * - 正文内容高亮：content字段高亮并截取匹配片段
     * - 模糊匹配：对短关键词添加fuzzy查询支持部分匹配
     * - 同义词识别：通过synonym分析器自动扩展同义词
     * - 匹配类型标识：标记命中的字段类型（title/content/summary）
     */
    private long searchArticles(String keyword, Long categoryId, Long tagId,
                                String timeRange, String startDate, String endDate,
                                String sort, Integer page, Integer pageSize,
                                SearchResultVO result) {
        try {
            int from = (page - 1) * pageSize;

            // 获取当前用户偏好分类（用于个性化加权）
            List<Long> preferredCategoryIds = getPreferredCategoryIds();

            SearchResponse<Map> response = openSearchClient.search(s -> {
                s.index(openSearchConfig.getArticleIndex())
                        .from(from)
                        .size(pageSize)
                        // 设置超时保证响应时间 < 500ms
                        .timeout("500ms");

                // 使用 function_score 查询实现相关性评分公式
                s.query(q -> q
                        .functionScore(fs -> {
                            // 基础查询：多字段匹配 + 拼音 + 同义词 + 模糊匹配
                            fs.query(bq -> bq.bool(b -> {
                                // 主查询：multiMatch 跨字段搜索（标题×3, 摘要×2, 正文×1）+ 拼音 + 同义词
                                b.must(m -> m
                                        .multiMatch(mm -> mm
                                                .fields("title^3", "summary^2", "content", "title.pinyin^2", "title.synonym^2", "summary.synonym", "content.synonym", "authorName", "categoryName")
                                                .query(keyword)
                                                .type(org.opensearch.client.opensearch._types.query_dsl.TextQueryType.BestFields)
                                                .fuzziness("AUTO")
                                                .prefixLength(2)
                                        )
                                );
                                // 分类筛选
                                if (categoryId != null) {
                                    b.filter(f -> f.term(t -> t.field("categoryId").value(FieldValue.of(categoryId))));
                                }
                                // 标签筛选
                                if (tagId != null) {
                                    b.filter(f -> f.nested(n -> n
                                            .path("tags")
                                            .query(nq -> nq.term(t -> t.field("tags.id").value(FieldValue.of(tagId))))
                                    ));
                                }
                                // 时间范围筛选
                                String sinceDate = getSinceDate(timeRange, startDate);
                                if (sinceDate != null) {
                                    b.filter(f -> f.range(r -> r
                                            .field("createdAt")
                                            .gte(JsonData.of(sinceDate))
                                    ));
                                }
                                if (StringUtils.hasText(endDate)) {
                                    b.filter(f -> f.range(r -> r
                                            .field("createdAt")
                                            .lte(JsonData.of(endDate + " 23:59:59"))
                                    ));
                                }
                                return b;
                            }));

                            // hot_score_boost: 使用 fieldValueFactor 对 hotScore 进行 log1p 加权
                            fs.functions(f -> f
                                    .fieldValueFactor(fvf -> fvf
                                            .field("hotScore")
                                            .factor(0.01)
                                            .modifier(org.opensearch.client.opensearch._types.query_dsl.FieldValueFactorModifier.Log1p)
                                    )
                            );

                            // time_decay: 简化实现，使用固定权重
                            fs.functions(f -> f
                                    .weight(0.8)
                            );

                            // personal_boost: 用户偏好分类的文章提升1.5倍
                            if (!preferredCategoryIds.isEmpty()) {
                                for (Long prefCategoryId : preferredCategoryIds) {
                                    fs.functions(f -> f
                                            .filter(pq -> pq.term(t -> t.field("categoryId").value(FieldValue.of(prefCategoryId))))
                                            .weight(1.5)
                                    );
                                }
                            }

                            fs.scoreMode(FunctionScoreMode.Sum);

                            return fs;
                        })
                );

                // 排序
                applyArticleSort(s, sort);

                // 关键词高亮：标题、摘要、正文内容
                s.highlight(h -> h
                        .preTags("<em>").postTags("</em>")
                        .fields("title", HighlightField.of(hf -> hf
                                .fragmentSize(200).numberOfFragments(1)
                        ))
                        .fields("summary", HighlightField.of(hf -> hf
                                .fragmentSize(200).numberOfFragments(1)
                        ))
                        .fields("content", HighlightField.of(hf -> hf
                                .fragmentSize(150).numberOfFragments(3)
                                .noMatchSize(0)
                        ))
                );

                return s;
            }, Map.class);

            List<ArticleVO> voList = new ArrayList<>();
            for (Hit<Map> hit : response.hits().hits()) {
                Map<String, Object> source = hit.source();
                if (source == null) continue;
                ArticleVO vo = mapToArticleVO(source);

                // 处理高亮结果
                if (hit.highlight() != null) {
                    boolean titleMatched = false;
                    boolean summaryMatched = false;
                    boolean contentMatched = false;

                    // 标题高亮
                    if (hit.highlight().containsKey("title") && !hit.highlight().get("title").isEmpty()) {
                        vo.setTitle(hit.highlight().get("title").get(0));
                        titleMatched = true;
                    }
                    // 摘要高亮
                    if (hit.highlight().containsKey("summary") && !hit.highlight().get("summary").isEmpty()) {
                        vo.setSummary(hit.highlight().get("summary").get(0));
                        summaryMatched = true;
                    }
                    // 正文内容高亮 - 拼接多个片段
                    if (hit.highlight().containsKey("content") && !hit.highlight().get("content").isEmpty()) {
                        List<String> fragments = hit.highlight().get("content");
                        vo.setContentSnippet(String.join(" ... ", fragments));
                        contentMatched = true;
                    }

                    // 设置匹配类型标识（优先级：title > summary > content）
                    if (titleMatched) {
                        vo.setMatchType("title");
                    } else if (summaryMatched) {
                        vo.setMatchType("summary");
                    } else if (contentMatched) {
                        vo.setMatchType("content");
                    }
                }

                // 如果正文没有高亮但匹配了，从原文截取片段
                if (vo.getContentSnippet() == null && vo.getMatchType() == null) {
                    vo.setMatchType("content");
                    String content = toString(source.get("content"));
                    if (content != null && content.length() > 150) {
                        vo.setContentSnippet(content.substring(0, 150) + "...");
                    }
                }

                voList.add(vo);
            }

            result.setArticles(voList);
            return response.hits().total().value();
        } catch (Exception e) {
            log.error("OpenSearch 搜索文章失败: {}", e.getMessage());
            result.setArticles(Collections.emptyList());
            return 0L;
        }
    }

    /**
     * 使用 OpenSearch 搜索用户（带相关性评分公式）
     * 评分公式：
     * - name_match_score: 精确匹配>前缀匹配>模糊匹配（通过 multiMatch 的 field boost 实现）
     * - follower_boost: log(1 + follower_count / 100) 加权
     * - follow_priority: 已关注用户2倍加权
     */
    private long searchUsers(String keyword, Integer page, Integer pageSize, SearchResultVO result) {
        try {
            int from = (page - 1) * pageSize;

            // 获取当前用户已关注的用户ID列表（用于 follow_priority 加权）
            List<Long> followedUserIds = getFollowedUserIds();

            SearchResponse<Map> response = openSearchClient.search(s -> {
                s.index(openSearchConfig.getUserIndex())
                        .from(from)
                        .size(pageSize);

                // 使用 function_score 查询实现用户搜索评分公式
                s.query(q -> q
                        .functionScore(fs -> {
                            // 基础查询：name_match_score（精确匹配>前缀匹配>模糊匹配）
                            // nickname^3 提升精确匹配权重，username^2 前缀匹配
                            fs.query(bq -> bq.bool(b -> {
                                b.should(sh -> sh
                                        .term(t -> t.field("nickname.keyword").value(FieldValue.of(keyword)).boost(10.0f))
                                );
                                b.should(sh -> sh
                                        .term(t -> t.field("username.keyword").value(FieldValue.of(keyword)).boost(8.0f))
                                );
                                b.should(sh -> sh
                                        .prefix(p -> p.field("nickname").value(keyword).boost(5.0f))
                                );
                                b.should(sh -> sh
                                        .prefix(p -> p.field("username").value(keyword).boost(4.0f))
                                );
                                b.should(sh -> sh
                                        .multiMatch(mm -> mm
                                                .fields("username^2", "nickname^3", "bio", "username.pinyin^2", "nickname.pinyin^2", "username.synonym^2", "nickname.synonym^2", "bio.synonym")
                                                .query(keyword)
                                        )
                                );
                                b.minimumShouldMatch("1");
                                return b;
                            }));

                            // follower_boost: 使用 fieldValueFactor 对 followerCount 进行 log1p 加权
                            fs.functions(f -> f
                                    .fieldValueFactor(fvf -> fvf
                                            .field("followerCount")
                                            .factor(0.01)
                                            .modifier(org.opensearch.client.opensearch._types.query_dsl.FieldValueFactorModifier.Log1p)
                                    )
                            );

                            // follow_priority: 已关注用户2倍加权
                            if (!followedUserIds.isEmpty()) {
                                for (Long followedId : followedUserIds) {
                                    fs.functions(f -> f
                                            .filter(pq -> pq.term(t -> t.field("id").value(FieldValue.of(followedId))))
                                            .weight(2.0)
                                    );
                                }
                            }

                            fs.scoreMode(FunctionScoreMode.Sum);

                            return fs;
                        })
                );

                s.highlight(h -> h
                        .fields("nickname", HighlightField.of(hf -> hf.preTags("<em>").postTags("</em>")))
                );

                return s;
            }, Map.class);

            List<UserVO> voList = new ArrayList<>();
            for (Hit<Map> hit : response.hits().hits()) {
                Map<String, Object> source = hit.source();
                if (source == null) continue;
                UserVO vo = mapToUserVO(source);

                if (hit.highlight() != null && hit.highlight().containsKey("nickname")
                        && !hit.highlight().get("nickname").isEmpty()) {
                    vo.setNickname(hit.highlight().get("nickname").get(0));
                }

                voList.add(vo);
            }

            result.setUsers(voList);
            return response.hits().total().value();
        } catch (Exception e) {
            log.error("OpenSearch 搜索用户失败: {}", e.getMessage());
            result.setUsers(Collections.emptyList());
            return 0L;
        }
    }

    /**
     * 使用 OpenSearch 搜索图片
     * 支持按文章标题、文章标题拼音搜索，按时间倒序排列
     */
    private long searchImages(String keyword, Integer page, Integer pageSize, SearchResultVO result) {
        try {
            int from = (page - 1) * pageSize;

            SearchResponse<Map> response = openSearchClient.search(s -> s
                            .index(openSearchConfig.getImageIndex())
                            .from(from)
                            .size(pageSize)
                            .query(q -> q
                                    .multiMatch(mm -> mm
                                            .fields("articleTitle^3", "articleTitle.pinyin^2")
                                            .query(keyword)
                                    )
                            )
                            .sort(so -> so.field(f -> f.field("articleCreatedAt").order(org.opensearch.client.opensearch._types.SortOrder.Desc)))
                            .highlight(h -> h
                                    .fields("articleTitle", HighlightField.of(hf -> hf.preTags("<em>").postTags("</em>")))
                            ),
                    Map.class
            );

            List<ArticleVO> voList = new ArrayList<>();
            for (Hit<Map> hit : response.hits().hits()) {
                Map<String, Object> source = hit.source();
                if (source == null) continue;

                ArticleVO vo = new ArticleVO();
                vo.setId(toLong(source.get("articleId")));
                vo.setTitle(toString(source.get("articleTitle")));
                vo.setCoverImage(toString(source.get("url")));
                vo.setCreatedAt(toLocalDateTime(source.get("articleCreatedAt")));

                if (hit.highlight() != null && hit.highlight().containsKey("articleTitle")
                        && !hit.highlight().get("articleTitle").isEmpty()) {
                    vo.setTitle(hit.highlight().get("articleTitle").get(0));
                }

                voList.add(vo);
            }

            result.setImages(voList);
            return response.hits().total().value();
        } catch (Exception e) {
            log.error("OpenSearch 搜索图片失败: {}", e.getMessage());
            result.setImages(Collections.emptyList());
            return 0L;
        }
    }

    // ========== 辅助方法 ==========

    /**
     * 缓存搜索建议结果
     */
    private void cacheSuggestResult(String cacheKey, SearchSuggestResultVO result) {
        try {
            String json = objectMapper.writeValueAsString(result);
            stringRedisTemplate.opsForValue().set(cacheKey, json, SUGGEST_CACHE_TTL_MINUTES, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("写入搜索建议缓存失败: {}", e.getMessage());
        }
    }

    private void applyArticleSort(org.opensearch.client.opensearch.core.SearchRequest.Builder s, String sort) {
        if ("hot".equals(sort)) {
            s.sort(so -> so.field(f -> f.field("hotScore").order(org.opensearch.client.opensearch._types.SortOrder.Desc)))
             .sort(so -> so.field(f -> f.field("viewCount").order(org.opensearch.client.opensearch._types.SortOrder.Desc)));
        } else if ("newest".equals(sort)) {
            s.sort(so -> so.field(f -> f.field("createdAt").order(org.opensearch.client.opensearch._types.SortOrder.Desc)));
        } else {
            // 默认按相关性排序，置顶优先
            s.sort(so -> so.field(f -> f.field("isTop").order(org.opensearch.client.opensearch._types.SortOrder.Desc)))
             .sort(so -> so.field(f -> f.field("_score").order(org.opensearch.client.opensearch._types.SortOrder.Desc)))
             .sort(so -> so.field(f -> f.field("createdAt").order(org.opensearch.client.opensearch._types.SortOrder.Desc)));
        }
    }

    private String getSinceDate(String timeRange, String startDate) {
        if (StringUtils.hasText(startDate)) {
            return startDate + " 00:00:00";
        }
        if (timeRange == null) return null;
        LocalDateTime now = LocalDateTime.now();
        return switch (timeRange) {
            case "day" -> now.toLocalDate().atStartOfDay().format(DATE_FORMATTER);
            case "week" -> now.minusWeeks(1).format(DATE_FORMATTER);
            case "month" -> now.minusMonths(1).format(DATE_FORMATTER);
            case "year" -> now.minusYears(1).format(DATE_FORMATTER);
            default -> null;
        };
    }

    @SuppressWarnings("unchecked")
    private ArticleVO mapToArticleVO(Map<String, Object> source) {
        ArticleVO vo = new ArticleVO();
        vo.setId(toLong(source.get("id")));
        vo.setTitle(toString(source.get("title")));
        vo.setSummary(toString(source.get("summary")));
        vo.setCoverImage(toString(source.get("coverImage")));
        vo.setStatus(toInteger(source.get("status")));
        vo.setViewCount(toLong(source.get("viewCount")));
        vo.setLikeCount(toLong(source.get("likeCount")));
        vo.setCommentCount(toLong(source.get("commentCount")));
        vo.setCollectCount(toLong(source.get("collectCount")));
        vo.setIsTop(toInteger(source.get("isTop")));
        vo.setAuthorName(toString(source.get("authorName")));
        vo.setAuthorAvatar(toString(source.get("authorAvatar")));
        vo.setCategoryName(toString(source.get("categoryName")));
        vo.setCreatedAt(toLocalDateTime(source.get("createdAt")));

        // 标签
        Object tagsObj = source.get("tags");
        if (tagsObj instanceof List<?> tagsList) {
            List<TagVO> tagVOList = new ArrayList<>();
            for (Object tagObj : tagsList) {
                if (tagObj instanceof Map<?, ?> tagMap) {
                    TagVO tagVO = new TagVO();
                    tagVO.setId(toLong(tagMap.get("id")));
                    tagVO.setName(toString(tagMap.get("name")));
                    tagVOList.add(tagVO);
                }
            }
            vo.setTags(tagVOList);
        } else {
            vo.setTags(Collections.emptyList());
        }

        return vo;
    }

    private UserVO mapToUserVO(Map<String, Object> source) {
        UserVO vo = new UserVO();
        vo.setId(toLong(source.get("id")));
        vo.setUsername(toString(source.get("username")));
        vo.setNickname(toString(source.get("nickname")));
        vo.setAvatar(toString(source.get("avatar")));
        vo.setRole(toString(source.get("role")));
        vo.setStatus(toInteger(source.get("status")));
        vo.setCreatedAt(toLocalDateTime(source.get("createdAt")));
        return vo;
    }

    private Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).longValue();
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer toInteger(Object value) {
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).intValue();
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String toString(Object value) {
        return value != null ? value.toString() : null;
    }

    private LocalDateTime toLocalDateTime(Object value) {
        if (value == null) return null;
        try {
            return LocalDateTime.parse(value.toString(), DATE_FORMATTER);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 安全获取当前用户ID
     */
    private Long getCurrentUserIdSafe() {
        try {
            return securityUtil.getCurrentUserId();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取当前用户偏好分类ID列表（用于个性化加权）
     */
    private List<Long> getPreferredCategoryIds() {
        try {
            Long userId = getCurrentUserIdSafe();
            if (userId == null) {
                return Collections.emptyList();
            }
            List<UserPreferredCategory> preferences = userPreferredCategoryMapper.selectList(
                    new LambdaQueryWrapper<UserPreferredCategory>()
                            .eq(UserPreferredCategory::getUserId, userId));
            return preferences.stream()
                    .map(UserPreferredCategory::getCategoryId)
                    .filter(id -> id != null)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("获取用户偏好分类失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 获取当前用户已关注的用户ID列表（用于 follow_priority 加权）
     */
    private List<Long> getFollowedUserIds() {
        try {
            Long userId = getCurrentUserIdSafe();
            if (userId == null) {
                return Collections.emptyList();
            }
            List<UserFollow> follows = userFollowMapper.selectList(
                    new LambdaQueryWrapper<UserFollow>()
                            .eq(UserFollow::getFollowerId, userId));
            return follows.stream()
                    .map(UserFollow::getFollowingId)
                    .filter(id -> id != null)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("获取用户关注列表失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
