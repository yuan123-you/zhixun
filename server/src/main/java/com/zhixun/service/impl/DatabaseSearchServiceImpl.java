package com.zhixun.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhixun.entity.Article;
import com.zhixun.entity.ArticleImage;
import com.zhixun.entity.Tag;
import com.zhixun.entity.User;
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
import org.opensearch.client.OpenSearchClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 搜索服务Fallback实现（基于数据库，当OpenSearch不可用时使用）
 * <p>
 * 增强功能：
 * - 作品搜索：标题 + 摘要 + 正文 + 作者名 + 分类名 多字段模糊匹配
 * - 用户搜索：昵称 + 用户名 + UID 多字段模糊匹配
 * - 图片搜索：按作品标题匹配
 * - 搜索建议：作品标题 + 用户昵称 + 标签名
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnMissingBean(OpenSearchClient.class)
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

        String searchType = type != null ? type : "all";

        switch (searchType) {
            case "article":
                long articleTotal = searchArticles(keyword, page, pageSize, result);
                result.setArticleTotal(articleTotal);
                result.setTotal(articleTotal);
                break;
            case "user":
                long userTotal = searchUsers(keyword, page, pageSize, result);
                result.setUserTotal(userTotal);
                result.setTotal(userTotal);
                break;
            case "image":
                long imageTotal = searchImages(keyword, page, pageSize, result);
                result.setImageTotal(imageTotal);
                result.setTotal(imageTotal);
                break;
            case "all":
            default:
                long aTotal = searchArticles(keyword, page, pageSize, result);
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
     * 作品搜索：标题 + 摘要 + 正文 + 作者名 + 分类名 多字段模糊匹配
     * MyBatis-Plus 的 .like() 自动添加 %keyword% 实现模糊搜索
     */
    private long searchArticles(String keyword, Integer page, Integer pageSize, SearchResultVO result) {
        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<>();
        // 多字段模糊搜索：标题、摘要、正文、作者名、分类名
        wrapper.and(w -> w
                .like(Article::getTitle, keyword)
                .or().like(Article::getSummary, keyword)
                .or().like(Article::getContent, keyword)
                .or().like(Article::getAuthorName, keyword)
                .or().like(Article::getCategoryName, keyword)
        );
        wrapper.eq(Article::getStatus, 1);
        wrapper.isNull(Article::getDeletedAt);
        // 排序：置顶优先，再按时间倒序
        wrapper.orderByDesc(Article::getIsTop);
        wrapper.orderByDesc(Article::getCreatedAt);

        long total = articleMapper.selectCount(wrapper);
        wrapper.last("LIMIT " + (page - 1) * pageSize + ", " + pageSize);
        List<Article> articles = articleMapper.selectList(wrapper);

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
     * 注意：UID 精确匹配使用 eq，模糊使用 like 进行前缀/模糊匹配
     */
    private long searchUsers(String keyword, Integer page, Integer pageSize, SearchResultVO result) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        // 多字段模糊搜索：昵称、用户名、UID
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
        // 先搜索作品标题匹配的作品ID
        LambdaQueryWrapper<Article> articleWrapper = new LambdaQueryWrapper<>();
        articleWrapper.like(Article::getTitle, keyword)
                .eq(Article::getStatus, 1)
                .isNull(Article::getDeletedAt)
                .select(Article::getId);
        List<Article> matchedArticles = articleMapper.selectList(articleWrapper);

        if (matchedArticles.isEmpty()) {
            result.setImages(Collections.emptyList());
            return 0L;
        }

        List<Long> articleIds = matchedArticles.stream().map(Article::getId).collect(Collectors.toList());

        // 查询这些作品的图片
        LambdaQueryWrapper<ArticleImage> imageWrapper = new LambdaQueryWrapper<>();
        imageWrapper.in(ArticleImage::getArticleId, articleIds);

        long total = articleImageMapper.selectCount(imageWrapper);
        imageWrapper.last("LIMIT " + (page - 1) * pageSize + ", " + pageSize);
        List<ArticleImage> images = articleImageMapper.selectList(imageWrapper);

        // 构建作品ID到标题的映射
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
            // 搜索作品标题
            LambdaQueryWrapper<Article> articleWrapper = new LambdaQueryWrapper<>();
            articleWrapper.like(Article::getTitle, keyword)
                    .eq(Article::getStatus, 1)
                    .isNull(Article::getDeletedAt)
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
            ).last("LIMIT 3");
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
        result.setHotSearches(Collections.emptyList());
        return result;
    }

    @Override
    public List<String> getHotSearches() {
        return Collections.emptyList();
    }

    @Override
    public void clearSearchHistory(Long userId) {
        searchHistoryService.clearSearchHistory(userId);
    }
}
