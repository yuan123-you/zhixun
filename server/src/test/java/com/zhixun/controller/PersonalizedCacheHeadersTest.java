package com.zhixun.controller;

import com.zhixun.common.util.SecurityUtil;
import com.zhixun.dto.article.ArticleQueryRequest;
import com.zhixun.service.ArticleService;
import com.zhixun.service.FeedService;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PersonalizedCacheHeadersTest {
    @AfterEach void reset() { SecurityContextHolder.clearContext(); }

    private MockHttpServletResponse call(String endpoint) {
        var security = new SecurityUtil();
        var response = new MockHttpServletResponse();
        var articles = new ArticleController(mock(ArticleService.class), security);
        var feeds = new FeedController(mock(FeedService.class), security);
        switch (endpoint) {
            case "articles" -> articles.list(new ArticleQueryRequest(), response);
            case "detail" -> articles.detail(10L, response);
            case "latest" -> feeds.latest(1, 10, response);
            case "hot" -> feeds.hot(1, 10, response);
            case "recommend" -> feeds.recommend(0, 1, 10, response);
            default -> throw new IllegalArgumentException(endpoint);
        }
        return response;
    }

    @ParameterizedTest
    @ValueSource(strings = {"articles", "detail", "latest", "hot", "recommend"})
    void authenticatedContentCannotBeStoredBySharedCaches(String endpoint) {
        var auth = new UsernamePasswordAuthenticationToken("viewer", null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        auth.setDetails(7L); SecurityContextHolder.getContext().setAuthentication(auth);
        var response = call(endpoint);
        assertEquals("private, no-store", response.getHeader("Cache-Control"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"articles", "detail", "latest", "hot", "recommend"})
    void anonymousPublicVariantsVaryByTheAuthenticationInputs(String endpoint) {
        var response = call(endpoint);
        assertTrue(response.getHeader("Cache-Control").startsWith("public,"));
        String vary = String.join(",", response.getHeaders("Vary")).toLowerCase();
        assertTrue(vary.contains("authorization"));
        assertTrue(vary.contains("cookie"));
    }
}
