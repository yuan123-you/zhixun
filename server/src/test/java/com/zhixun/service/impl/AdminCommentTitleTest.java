package com.zhixun.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhixun.entity.Article;
import com.zhixun.entity.Comment;
import com.zhixun.mapper.ArticleMapper;
import com.zhixun.mapper.CommentMapper;
import com.zhixun.mapper.UserMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminCommentTitleTest {
    @Mock CommentMapper comments;
    @Mock ArticleMapper articles;
    @Mock UserMapper users;
    @InjectMocks AdminServiceImpl service;

    private Comment comment() {
        Comment comment = new Comment();
        comment.setId(1L); comment.setArticleId(10L); comment.setUserId(7L);
        comment.setContent("Comment");
        return comment;
    }

    @Test
    void adminCommentListContainsJoinedArticleTitle() {
        Page<Comment> page = new Page<>(1, 10);
        page.setRecords(List.of(comment())); page.setTotal(1);
        Article article = new Article(); article.setId(10L); article.setTitle("保留的作品标题");
        when(comments.selectPage(any(Page.class), any())).thenReturn(page);
        when(users.selectBatchIds(anyCollection())).thenReturn(List.of());
        when(articles.selectBatchIds(anyCollection())).thenReturn(List.of(article));
        var result = service.getCommentList(null, null, 1, 10);
        var json = new ObjectMapper().valueToTree(result);
        assertEquals("保留的作品标题", json.path("list").get(0).path("articleTitle").asText());
        verify(articles).selectBatchIds(anyCollection());
    }

    @Test
    void missingArticleKeepsTheExistingEmptyTitleFallback() {
        Page<Comment> page = new Page<>(1, 10);
        page.setRecords(List.of(comment())); page.setTotal(1);
        when(comments.selectPage(any(Page.class), any())).thenReturn(page);
        when(users.selectBatchIds(anyCollection())).thenReturn(List.of());
        when(articles.selectBatchIds(anyCollection())).thenReturn(List.of());
        var result = service.getCommentList(null, null, 1, 10);
        var json = new ObjectMapper().valueToTree(result);
        assertEquals("", json.path("list").get(0).path("articleTitle").asText());
    }
}
