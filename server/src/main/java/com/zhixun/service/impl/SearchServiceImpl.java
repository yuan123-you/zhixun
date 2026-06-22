package com.zhixun.service.impl;

import com.zhixun.config.OpenSearchConfig;
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
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.search.HighlightField;
import org.opensearch.client.opensearch.core.search.Hit;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
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
                break;
            case "user":
                total = searchUsers(keyword, page, pageSize, result);
                break;
            case "image":
                total = searchImages(keyword, page, pageSize, result);
                break;
            case "all":
            default:
                searchArticles(keyword, categoryId, tagId, timeRange, startDate, endDate, sort, page, pageSize, result);
                searchUsers(keyword, 1, 5, result);
                searchImages(keyword, 1, 6, result);
                total = (result.getArticles() != null ? result.getArticles().size() : 0)
                        + (result.getUsers() != null ? result.getUsers().size() : 0)
                        + (result.getImages() != null ? result.getImages().size() : 0);
                break;
        }

        result.setTotal(total);
        result.setTook(System.currentTimeMillis() - startTime);
        return result;
    }

    @Override
    public SearchSuggestResultVO getSuggestions(String keyword) {
        SearchSuggestResultVO result = new SearchSuggestResultVO();
        List<SuggestionVO> completions = new ArrayList<>();

        if (!StringUtils.hasText(keyword) || keyword.length() < 2) {
            result.setCompletions(completions);
            result.setHotSearches(searchHistoryService.getHotSearchKeywords(5));
            return result;
        }

        try {
            // 匹配用户名/昵称（2条）
            SearchResponse<Map> userResponse = openSearchClient.search(s -> s
                            .index(openSearchConfig.getUserIndex())
                            .size(2)
                            .query(q -> q
                                    .multiMatch(mm -> mm
                                            .fields("username", "nickname", "username.pinyin", "nickname.pinyin")
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
                                            .fields("title", "title.pinyin")
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
        return result;
    }

    @Override
    public List<String> getHotSearches() {
        return searchHistoryService.getHotSearchKeywords(10);
    }

    @Override
    public void clearSearchHistory(Long userId) {
        searchHistoryService.clearSearchHistory(userId);
    }

    // ========== 内部方法 ==========

    /**
     * 使用 OpenSearch 搜索文章
     */
    private long searchArticles(String keyword, Long categoryId, Long tagId,
                                String timeRange, String startDate, String endDate,
                                String sort, Integer page, Integer pageSize,
                                SearchResultVO result) {
        try {
            int from = (page - 1) * pageSize;

            SearchResponse<Map> response = openSearchClient.search(s -> {
                s.index(openSearchConfig.getArticleIndex())
                        .from(from)
                        .size(pageSize)
                        .query(q -> q
                                .bool(b -> {
                                    // 关键词搜索（标题/内容/摘要 + 拼音）
                                    b.must(m -> m
                                            .multiMatch(mm -> mm
                                                    .fields("title^3", "summary^2", "content", "title.pinyin^2", "authorName", "categoryName")
                                                    .query(keyword)
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
                                })
                        );

                // 排序
                applyArticleSort(s, sort);

                // 关键词高亮
                s.highlight(h -> h
                        .fields("title", HighlightField.of(hf -> hf.preTags("<em>").postTags("</em>")))
                        .fields("summary", HighlightField.of(hf -> hf.preTags("<em>").postTags("</em>")))
                );

                return s;
            }, Map.class);

            List<ArticleVO> voList = new ArrayList<>();
            for (Hit<Map> hit : response.hits().hits()) {
                Map<String, Object> source = hit.source();
                if (source == null) continue;
                ArticleVO vo = mapToArticleVO(source);

                // 使用高亮结果替换原始值
                if (hit.highlight() != null) {
                    if (hit.highlight().containsKey("title") && !hit.highlight().get("title").isEmpty()) {
                        vo.setTitle(hit.highlight().get("title").get(0));
                    }
                    if (hit.highlight().containsKey("summary") && !hit.highlight().get("summary").isEmpty()) {
                        vo.setSummary(hit.highlight().get("summary").get(0));
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
     * 使用 OpenSearch 搜索用户
     */
    private long searchUsers(String keyword, Integer page, Integer pageSize, SearchResultVO result) {
        try {
            int from = (page - 1) * pageSize;

            SearchResponse<Map> response = openSearchClient.search(s -> s
                            .index(openSearchConfig.getUserIndex())
                            .from(from)
                            .size(pageSize)
                            .query(q -> q
                                    .multiMatch(mm -> mm
                                            .fields("username^2", "nickname^3", "bio", "username.pinyin^2", "nickname.pinyin^2")
                                            .query(keyword)
                                    )
                            )
                            .highlight(h -> h
                                    .fields("nickname", HighlightField.of(hf -> hf.preTags("<em>").postTags("</em>")))
                            ),
                    Map.class
            );

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
            org.springframework.security.core.Authentication authentication =
                    org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                Object details = authentication.getDetails();
                if (details instanceof Long) {
                    return (Long) details;
                }
                if (details instanceof Integer) {
                    return ((Integer) details).longValue();
                }
            }
        } catch (Exception e) {
            // 未登录
        }
        return null;
    }
}
