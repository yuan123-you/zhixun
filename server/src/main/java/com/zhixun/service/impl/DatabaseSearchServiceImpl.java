package com.zhixun.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhixun.entity.Article;
import com.zhixun.entity.ArticleImage;
import com.zhixun.entity.Tag;
import com.zhixun.entity.User;
import com.zhixun.enums.ArticleStatusEnum;
import com.zhixun.mapper.ArticleImageMapper;
import com.zhixun.mapper.ArticleMapper;
import com.zhixun.mapper.TagMapper;
import com.zhixun.mapper.UserMapper;
import com.zhixun.service.SearchHistoryService;
import com.zhixun.service.SearchService;
import com.zhixun.vo.ArticleVO;
import com.zhixun.vo.SearchResultVO;
import com.zhixun.vo.SearchSuggestResultVO;
import com.zhixun.vo.SuggestionVO;
import com.zhixun.vo.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 搜索服务Fallback实现（基于数据库与智能相关度排序算法，当OpenSearch不可用时使用）
 * <p>
 * 增强功能：
 * - 作品搜索：标题 + 摘要 + 正文 多字段模糊匹配与加权相关度打分（Title: 100, Summary: 40, Content: 10 + 热度加权）
 * - 支持分类、时段过滤以及多种排序模式（综合/最热/点赞/最新）
 * - 用户搜索：昵称 + 用户名 + UID 多字段模糊匹配
 * - 图片搜索：按作品标题匹配
 * - 搜索建议：作品标题 + 用户昵称 + 标签名 + 热门搜索补全
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "opensearch.enabled", havingValue = "false", matchIfMissing = true)
public class DatabaseSearchServiceImpl implements SearchService {

    private final ArticleMapper articleMapper;
    private final UserMapper userMapper;
    private final TagMapper tagMapper;
    private final ArticleImageMapper articleImageMapper;
    private final SearchHistoryService searchHistoryService;

    @Override
    public SearchResultVO search(String keyword, String type, Long categoryId, Long tagId,
                                  String timeRange, String startDate, String endDate,
                                  String sort, Integer page, Integer pageSize) {
        SearchResultVO result = new SearchResultVO();
        result.setKeyword(keyword);
        result.setType(type);

        if (!StringUtils.hasText(keyword)) {
            result.setTotal(0L);
            return result;
        }

        int p = (page != null && page > 0) ? page : 1;
        int ps = (pageSize != null && pageSize > 0) ? pageSize : 20;

        // 规范化 type：兼容前端复数形式
        String normalizedType = type != null ? type : "all";
        if ("articles".equals(normalizedType)) normalizedType = "article";
        if ("users".equals(normalizedType)) normalizedType = "user";
        if ("images".equals(normalizedType)) normalizedType = "image";

        switch (normalizedType) {
            case "article":
                long articleTotal = searchArticles(keyword, categoryId, timeRange, sort, p, ps, result);
                result.setArticleTotal(articleTotal);
                result.setTotal(articleTotal);
                break;
            case "user":
                long userTotal = searchUsers(keyword, p, ps, result);
                result.setUserTotal(userTotal);
                result.setTotal(userTotal);
                break;
            case "image":
                long imageTotal = searchImages(keyword, p, ps, result);
                result.setImageTotal(imageTotal);
                result.setTotal(imageTotal);
                break;
            case "all":
            default:
                long aTotal = searchArticles(keyword, categoryId, timeRange, sort, p, ps, result);
                long uTotal = searchUsers(keyword, 1, 5, result);
                long iTotal = searchImages(keyword, 1, 6, result);
                result.setArticleTotal(aTotal);
                result.setUserTotal(uTotal);
                result.setImageTotal(iTotal);
                result.setTotal(aTotal + uTotal + iTotal);
                break;
        }

        return result;
    }

    /**
     * 作品搜索：多字段模糊匹配 + 多因子相关度排序 + 动态筛选
     */
    private long searchArticles(String keyword, Long categoryId, String timeRange, String sort,
                                Integer page, Integer pageSize, SearchResultVO result) {
        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<>();
        // 多字段模糊搜索：标题、摘要、正文
        wrapper.and(w -> w
                .like(Article::getTitle, keyword)
                .or().like(Article::getSummary, keyword)
                .or().like(Article::getContent, keyword)
        );
        wrapper.eq(Article::getStatus, ArticleStatusEnum.PUBLISHED);
        wrapper.isNull(Article::getDeletedAt);

        // 分类过滤
        if (categoryId != null && categoryId > 0) {
            wrapper.eq(Article::getCategoryId, categoryId);
        }

        // 时间范围过滤
        if (StringUtils.hasText(timeRange)) {
            LocalDateTime now = LocalDateTime.now();
            switch (timeRange.toLowerCase()) {
                case "day":
                    wrapper.ge(Article::getCreatedAt, now.minusDays(1));
                    break;
                case "week":
                    wrapper.ge(Article::getCreatedAt, now.minusWeeks(1));
                    break;
                case "month":
                    wrapper.ge(Article::getCreatedAt, now.minusMonths(1));
                    break;
                case "year":
                    wrapper.ge(Article::getCreatedAt, now.minusYears(1));
                    break;
                default:
                    break;
            }
        }

        long total = articleMapper.selectCount(wrapper);
        if (total == 0) {
            result.setArticles(Collections.emptyList());
            return 0L;
        }

        List<Article> articles;
        boolean isRelevance = !StringUtils.hasText(sort) || "relevance".equalsIgnoreCase(sort) || "all".equalsIgnoreCase(sort);

        if (isRelevance) {
            // 相关度排序：取前 100 条候选集在内存中按相关度打分重排
            int candidateLimit = Math.min((int) total, Math.max(page * pageSize, 100));
            wrapper.orderByDesc(Article::getIsTop);
            wrapper.last("LIMIT " + candidateLimit);
            List<Article> candidateList = articleMapper.selectList(wrapper);

            // 相关度打分器
            candidateList.sort((a, b) -> {
                int topCompare = Integer.compare(b.getIsTop() != null ? b.getIsTop() : 0, a.getIsTop() != null ? a.getIsTop() : 0);
                if (topCompare != 0) return topCompare;

                double scoreA = computeRelevanceScore(a, keyword);
                double scoreB = computeRelevanceScore(b, keyword);
                return Double.compare(scoreB, scoreA);
            });

            int fromIndex = Math.min((page - 1) * pageSize, candidateList.size());
            int toIndex = Math.min(fromIndex + pageSize, candidateList.size());
            articles = candidateList.subList(fromIndex, toIndex);
        } else {
            // 明确的字段排序
            wrapper.orderByDesc(Article::getIsTop);
            if ("hot".equalsIgnoreCase(sort) || "views".equalsIgnoreCase(sort)) {
                wrapper.orderByDesc(Article::getViewCount).orderByDesc(Article::getCreatedAt);
            } else if ("likes".equalsIgnoreCase(sort)) {
                wrapper.orderByDesc(Article::getLikeCount).orderByDesc(Article::getCreatedAt);
            } else {
                // newest / time / default
                wrapper.orderByDesc(Article::getCreatedAt);
            }
            wrapper.last("LIMIT " + (page - 1) * pageSize + ", " + pageSize);
            articles = articleMapper.selectList(wrapper);
        }

        List<ArticleVO> articleVOs = articles.stream().map(a -> {
            ArticleVO vo = new ArticleVO();
            vo.setId(a.getId());
            vo.setTitle(highlightMatch(a.getTitle(), keyword));
            vo.setSummary(highlightMatch(a.getSummary(), keyword));
            vo.setCoverImage(a.getCoverImage());
            vo.setViewCount(a.getViewCount());
            vo.setLikeCount(a.getLikeCount());
            vo.setCreatedAt(a.getCreatedAt());
            vo.setIsTop(a.getIsTop());
            vo.setAuthorName(a.getAuthorName());
            vo.setAuthorAvatar(a.getAuthorAvatar());
            vo.setCategoryName(a.getCategoryName());

            // 设置匹配类型标识
            if (a.getTitle() != null && a.getTitle().contains(keyword)) {
                vo.setMatchType("title");
            } else if (a.getSummary() != null && a.getSummary().contains(keyword)) {
                vo.setMatchType("summary");
            } else {
                vo.setMatchType("content");
            }

            // 从正文提取匹配片段
            if (a.getContent() != null) {
                String snippet = extractSnippet(a.getContent(), keyword, 150);
                vo.setContentSnippet(snippet != null ? highlightMatch(snippet, keyword) : null);
            }

            return vo;
        }).collect(Collectors.toList());

        result.setArticles(articleVOs);
        return total;
    }

    /**
     * 计算相关度得分：标题命中 > 摘要命中 > 正文命中 + 热度对数加分
     */
    private double computeRelevanceScore(Article article, String keyword) {
        double score = 0.0;
        String kwLower = keyword.toLowerCase();

        String title = article.getTitle() != null ? article.getTitle().toLowerCase() : "";
        if (title.equals(kwLower)) {
            score += 200.0; // 完全精确匹配
        } else if (title.startsWith(kwLower)) {
            score += 120.0; // 前缀匹配
        } else if (title.contains(kwLower)) {
            score += 80.0;  // 标题包含
        }

        String summary = article.getSummary() != null ? article.getSummary().toLowerCase() : "";
        if (summary.contains(kwLower)) {
            score += 30.0;
        }

        String content = article.getContent() != null ? article.getContent().toLowerCase() : "";
        if (content.contains(kwLower)) {
            score += 10.0;
        }

        // 热度加成因子：log10(views + likes * 2 + 1) * 3
        long views = article.getViewCount() != null ? article.getViewCount() : 0;
        long likes = article.getLikeCount() != null ? article.getLikeCount() : 0;
        double popularity = Math.log10(views + likes * 2.0 + 1.0) * 3.0;
        score += popularity;

        return score;
    }

    /**
     * 高亮匹配关键词（数据库降级方案）
     */
    private String highlightMatch(String text, String keyword) {
        if (text == null || keyword == null) return text;
        return text.replace(keyword, "<em>" + keyword + "</em>");
    }

    /**
     * 从正文中提取包含关键词的片段
     */
    private String extractSnippet(String content, String keyword, int maxLen) {
        if (content == null || keyword == null) return null;
        int idx = content.indexOf(keyword);
        if (idx < 0) return null;
        int start = Math.max(0, idx - maxLen / 3);
        int end = Math.min(content.length(), start + maxLen);
        if (start > 0) start = Math.max(0, content.lastIndexOf('。', start) + 1);
        String snippet = content.substring(start, end);
        if (end < content.length()) snippet += "...";
        if (start > 0) snippet = "..." + snippet;
        return snippet;
    }

    /**
     * 用户搜索：昵称 + 用户名 + UID 多字段模糊匹配
     */
    private long searchUsers(String keyword, Integer page, Integer pageSize, SearchResultVO result) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w
                .like(User::getNickname, keyword)
                .or().like(User::getUsername, keyword)
                .or().like(User::getUid, keyword)
        );

        long total = userMapper.selectCount(wrapper);
        wrapper.last("LIMIT " + (page - 1) * pageSize + ", " + pageSize);
        List<User> users = userMapper.selectList(wrapper);

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
        return total;
    }

    private long searchImages(String keyword, Integer page, Integer pageSize, SearchResultVO result) {
        LambdaQueryWrapper<Article> articleWrapper = new LambdaQueryWrapper<>();
        articleWrapper.like(Article::getTitle, keyword)
                .eq(Article::getStatus, ArticleStatusEnum.PUBLISHED)
                .isNull(Article::getDeletedAt)
                .select(Article::getId);
        List<Article> matchedArticles = articleMapper.selectList(articleWrapper);

        if (matchedArticles.isEmpty()) {
            result.setImages(Collections.emptyList());
            return 0L;
        }

        List<Long> articleIds = matchedArticles.stream().map(Article::getId).collect(Collectors.toList());

        LambdaQueryWrapper<ArticleImage> imageWrapper = new LambdaQueryWrapper<>();
        imageWrapper.in(ArticleImage::getArticleId, articleIds);

        long total = articleImageMapper.selectCount(imageWrapper);
        imageWrapper.last("LIMIT " + (page - 1) * pageSize + ", " + pageSize);
        List<ArticleImage> images = articleImageMapper.selectList(imageWrapper);

        var articleMap = matchedArticles.stream()
                .collect(Collectors.toMap(Article::getId, a -> a));

        List<ArticleVO> imageVOs = images.stream().map(img -> {
            Article article = articleMap.get(img.getArticleId());
            ArticleVO vo = new ArticleVO();
            vo.setId(img.getArticleId());
            vo.setTitle(article != null ? article.getTitle() : null);
            vo.setCoverImage(img.getUrl());
            vo.setCreatedAt(article != null ? article.getCreatedAt() : null);
            return vo;
        }).collect(Collectors.toList());

        result.setImages(imageVOs);
        return total;
    }

    @Override
    public SearchSuggestResultVO getSuggestions(String keyword) {
        List<SuggestionVO> completions = new ArrayList<>();

        if (StringUtils.hasText(keyword)) {
            // 搜索作品标题（必须为 PUBLISHED 状态）
            LambdaQueryWrapper<Article> articleWrapper = new LambdaQueryWrapper<>();
            articleWrapper.like(Article::getTitle, keyword)
                    .eq(Article::getStatus, ArticleStatusEnum.PUBLISHED)
                    .isNull(Article::getDeletedAt)
                    .orderByDesc(Article::getViewCount)
                    .last("LIMIT 5");
            List<Article> articles = articleMapper.selectList(articleWrapper);
            for (Article a : articles) {
                SuggestionVO vo = new SuggestionVO();
                vo.setType("article");
                vo.setId(a.getId());
                vo.setText(a.getTitle());
                completions.add(vo);
            }

            // 搜索用户（昵称 + 用户名 + UID）
            LambdaQueryWrapper<User> userWrapper = new LambdaQueryWrapper<>();
            userWrapper.and(w -> w
                    .like(User::getNickname, keyword)
                    .or().like(User::getUsername, keyword)
                    .or().like(User::getUid, keyword)
            ).orderByDesc(User::getFollowerCount)
             .last("LIMIT 3");
            List<User> users = userMapper.selectList(userWrapper);
            for (User u : users) {
                SuggestionVO vo = new SuggestionVO();
                vo.setType("user");
                vo.setId(u.getId());
                vo.setText(u.getNickname() != null ? u.getNickname() : u.getUsername());
                vo.setAvatar(u.getAvatar());
                completions.add(vo);
            }

            // 搜索标签
            LambdaQueryWrapper<Tag> tagWrapper = new LambdaQueryWrapper<>();
            tagWrapper.like(Tag::getName, keyword)
                    .last("LIMIT 3");
            List<Tag> tags = tagMapper.selectList(tagWrapper);
            for (Tag t : tags) {
                SuggestionVO vo = new SuggestionVO();
                vo.setType("tag");
                vo.setId(t.getId());
                vo.setText(t.getName());
                completions.add(vo);
            }
        }

        SearchSuggestResultVO result = new SearchSuggestResultVO();
        result.setCompletions(completions);
        result.setHotSearches(getHotSearches());
        return result;
    }

    @Override
    public List<String> getHotSearches() {
        if (searchHistoryService == null) {
            return Collections.emptyList();
        }
        try {
            List<String> hot = searchHistoryService.getHotSearchKeywords(10);
            return hot != null ? hot : Collections.emptyList();
        } catch (Exception e) {
            log.warn("读取热门搜索词失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public void clearSearchHistory(Long userId) {
        searchHistoryService.clearSearchHistory(userId);
    }
}
