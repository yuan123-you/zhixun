package com.zhixun.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhixun.common.result.PageResult;
import com.zhixun.entity.Article;
import com.zhixun.entity.Tag;
import com.zhixun.entity.User;
import com.zhixun.mapper.ArticleMapper;
import com.zhixun.mapper.TagMapper;
import com.zhixun.mapper.UserMapper;
import com.zhixun.service.SearchHistoryService;
import com.zhixun.service.SearchService;
import com.zhixun.vo.ArticleVO;
import com.zhixun.vo.SearchResultVO;
import com.zhixun.vo.SearchSuggestResultVO;
import com.zhixun.vo.SuggestionVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 搜索服务Fallback实现（基于数据库，当OpenSearch不可用时使用）
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "opensearch.enabled", havingValue = "false", matchIfMissing = false)
public class DatabaseSearchServiceImpl implements SearchService {

    private final ArticleMapper articleMapper;
    private final UserMapper userMapper;
    private final TagMapper tagMapper;
    private final SearchHistoryService searchHistoryService;

    @Override
    public SearchResultVO search(String keyword, String type, Long categoryId, Long tagId,
                                  String timeRange, String startDate, String endDate,
                                  String sort, Integer page, Integer pageSize) {
        if (!StringUtils.hasText(keyword)) {
            SearchResultVO result = new SearchResultVO();
            result.setArticles(Collections.emptyList());
            result.setTotal(0L);
            return result;
        }

        // 使用数据库模糊搜索
        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(Article::getTitle, keyword)
                .or()
                .like(Article::getSummary, keyword);

        wrapper.eq(Article::getStatus, 1); // 仅已发布
        wrapper.isNull(Article::getDeletedAt);

        long total = articleMapper.selectCount(wrapper);
        wrapper.last("LIMIT " + (page - 1) * pageSize + ", " + pageSize);
        List<Article> articles = articleMapper.selectList(wrapper);

        List<ArticleVO> articleVOs = articles.stream().map(a -> {
            ArticleVO vo = new ArticleVO();
            vo.setId(a.getId());
            vo.setTitle(a.getTitle());
            vo.setSummary(a.getSummary());
            return vo;
        }).collect(Collectors.toList());

        SearchResultVO result = new SearchResultVO();
        result.setKeyword(keyword);
        result.setType(type);
        result.setArticles(articleVOs);
        result.setTotal(total);
        return result;
    }

    @Override
    public SearchSuggestResultVO getSuggestions(String keyword) {
        List<SuggestionVO> completions = new ArrayList<>();

        if (StringUtils.hasText(keyword)) {
            // 搜索文章标题
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

            // 搜索用户
            LambdaQueryWrapper<User> userWrapper = new LambdaQueryWrapper<>();
            userWrapper.like(User::getNickname, keyword)
                    .last("LIMIT 3");
            List<User> users = userMapper.selectList(userWrapper);
            for (User u : users) {
                SuggestionVO vo = new SuggestionVO();
                vo.setType("user");
                vo.setId(u.getId());
                vo.setText(u.getNickname());
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
