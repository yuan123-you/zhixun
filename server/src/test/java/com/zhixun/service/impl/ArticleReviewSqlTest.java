package com.zhixun.service.impl;

import com.baomidou.mybatisplus.annotation.TableField;
import com.zhixun.common.util.SecurityUtil;
import com.zhixun.common.util.SensitiveWordUtil;
import com.zhixun.dto.article.*;
import com.zhixun.entity.Article;
import com.zhixun.mapper.*;
import com.zhixun.service.FeedService;
import com.zhixun.service.OpenSearchSyncService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ArticleReviewSqlTest {
    static class ReviewGate {
        final java.util.concurrent.CountDownLatch locked = new java.util.concurrent.CountDownLatch(1);
        final java.util.concurrent.CountDownLatch release = new java.util.concurrent.CountDownLatch(1);
        final java.util.concurrent.CountDownLatch editorEntered = new java.util.concurrent.CountDownLatch(1);
        boolean enabled;
    }
    @Configuration
    @org.springframework.transaction.annotation.EnableTransactionManagement(proxyTargetClass = true)
    static class Config extends BusinessSqlTransactionTest.Config {
        @Bean ReviewGate reviewGate() { return new ReviewGate(); }
        @Bean SensitiveWordUtil sensitive() {
            SensitiveWordUtil sensitive = mock(SensitiveWordUtil.class);
            when(sensitive.filter(anyString())).thenAnswer(inv -> inv.getArgument(0)); return sensitive;
        }
        @Override @Bean ArticleServiceImpl service(ArticleMapper articles, BusinessSqlTransactionTest.ReadHook hook,
                OpenSearchSyncService search, FeedService feed, StringRedisTemplate redis, RabbitTemplate broker) {
            SecurityUtil security = mock(SecurityUtil.class); when(security.isAdmin()).thenReturn(true);
            when(security.isOwnerOrAdmin(anyLong())).thenReturn(true);
            ReviewGate gate = reviewGate();
            var observed = (ArticleMapper) java.lang.reflect.Proxy.newProxyInstance(ArticleMapper.class.getClassLoader(),
                    new Class<?>[]{ArticleMapper.class}, (proxy, method, args) -> {
                        if (gate.enabled && method.getName().equals("selectForUpdate")
                                && Thread.currentThread().getName().equals("editing")) gate.editorEntered.countDown();
                        Object result;
                        try { result = method.invoke(articles, args); }
                        catch (java.lang.reflect.InvocationTargetException e) { throw e.getCause(); }
                        if (gate.enabled && method.getName().equals("selectForUpdate")
                                && Thread.currentThread().getName().equals("approving")) {
                            gate.locked.countDown();
                            if (!gate.release.await(5, java.util.concurrent.TimeUnit.SECONDS)) throw new IllegalStateException("review gate timeout");
                        }
                        return result;
                    });
            return new ArticleServiceImpl(observed, mock(ArticleTagMapper.class), mock(ArticleLikeMapper.class),
                    mock(ArticleViewHistoryMapper.class), mock(CollectMapper.class), mock(UserFollowMapper.class),
                    mock(ArticleOperateLogMapper.class), mock(UserMapper.class), mock(CategoryMapper.class), mock(TagMapper.class),
                    mock(ArticleImageMapper.class), sensitive(), security, search, feed, redis, broker, new ObjectMapper());
        }
    }
    private AnnotationConfigApplicationContext context;
    private JdbcTemplate jdbc;
    private ArticleServiceImpl service;
    private OpenSearchSyncService search;
    private FeedService feed;
    @BeforeEach void setUp() {
        context = new AnnotationConfigApplicationContext(Config.class);
        jdbc = context.getBean(JdbcTemplate.class); service = context.getBean(ArticleServiceImpl.class);
        search = context.getBean(OpenSearchSyncService.class); feed = context.getBean(FeedService.class);
        List<String> columns = new ArrayList<>(); boolean reviewField = false;
        for (var field : Article.class.getDeclaredFields()) {
            var annotation = field.getAnnotation(TableField.class);
            if (Modifier.isStatic(field.getModifiers()) || (annotation != null && !annotation.exist())) continue;
            String name = field.getName().replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase(Locale.ROOT);
            if (name.equals("review_state")) reviewField = true;
            Class<?> type = field.getType();
            String sql = type == Long.class ? "bigint" : type == Integer.class || type.isEnum() ? "integer"
                    : type == LocalDateTime.class ? "timestamp" : type == BigDecimal.class ? "decimal(18,4)" : "varchar(4096)";
            columns.add(name.equals("id") ? "id bigint generated by default as identity primary key" : name + " " + sql);
        }
        if (!reviewField) columns.add("review_state varchar(16)");
        jdbc.execute("create table cms_article(" + String.join(",", columns) + ")");
        jdbc.update("insert into cms_article(id,author_id,title,content,status,publish_at,review_state) values(10,7,'Original','Content',1,?,'REQUIRED')", LocalDateTime.now().minusHours(1).withNano(0));
        clearInvocations(search, feed, context.getBean(StringRedisTemplate.class), context.getBean(RabbitTemplate.class));
    }
    @AfterEach void tearDown() { jdbc.execute("drop all objects"); context.close(); }
    private void noPublicationEffects() {
        verify(search, never()).syncArticle(anyLong());
        verify(search, never()).syncImagesByArticle(anyLong());
        verify(feed, never()).fanoutOnPublish(anyLong(), anyLong(), any());
    }
    private int status() { return jdbc.queryForObject("select status from cms_article where id=10", Integer.class); }
    private String state() { return jdbc.queryForObject("select review_state from cms_article where id=10", String.class); }
    private ArticleStatusRequest approval() { var request = new ArticleStatusRequest(); request.setStatus(2); return request; }

    @ParameterizedTest @ValueSource(strings={"REQUIRED", "UNVERIFIED"})
    void actualSqlCannotPublishUnapprovedDueRows(String review) {
        jdbc.update("update cms_article set review_state=? where id=10", review);
        service.publishScheduledArticles(); assertEquals(1, status()); assertEquals(review, state());
        noPublicationEffects();
    }
    @ParameterizedTest @ValueSource(strings={"CLEARED", "APPROVED"})
    void actualSqlAllowsAuthorizedDueRows(String review) {
        jdbc.update("update cms_article set review_state=? where id=10", review);
        service.publishScheduledArticles(); assertEquals(2, status()); assertEquals(review, state());
    }
    @Test void approvalPersistsPermissionButDoesNotPublishBeforeTheDate() {
        LocalDateTime future = LocalDateTime.now().plusDays(1).withNano(0);
        jdbc.update("update cms_article set publish_at=?, reject_reason='Old reason' where id=10", future);
        service.updateArticleStatus(99L, 10L, approval());
        assertEquals(1, status()); assertEquals("APPROVED", state());
        assertNull(jdbc.queryForObject("select reject_reason from cms_article where id=10", String.class));
        assertEquals(future, jdbc.queryForObject("select publish_at from cms_article where id=10", java.sql.Timestamp.class).toLocalDateTime());
        service.publishScheduledArticles(); assertEquals(1, status()); noPublicationEffects();
        jdbc.update("update cms_article set publish_at=? where id=10", LocalDateTime.now().minusMinutes(1));
        service.publishScheduledArticles(); assertEquals(2, status());
    }
    @Test void changingReviewedContentInvalidatesPersistedPermission() {
        service.updateArticleStatus(99L, 10L, approval()); assertEquals(2, status());
        var edit = new ArticleUpdateRequest(); edit.setTitle("New title"); edit.setContent("New content");
        service.updateArticle(7L, 10L, edit);
        assertEquals(1, status()); assertEquals("REQUIRED", state());
        service.publishScheduledArticles(); assertEquals(1, status());
    }
    @Test void concurrentApprovalAndEditCannotApproveAContentVersionItDidNotRead() throws Exception {
        assertTrue(org.springframework.aop.support.AopUtils.isAopProxy(service));
        ReviewGate gate = context.getBean(ReviewGate.class); gate.enabled = true;
        var executor = java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            var approve = executor.submit(() -> { Thread.currentThread().setName("approving"); service.updateArticleStatus(99L, 10L, approval()); });
            assertTrue(gate.locked.await(5, java.util.concurrent.TimeUnit.SECONDS));
            var edit = executor.submit(() -> {
                Thread.currentThread().setName("editing");
                var request = new ArticleUpdateRequest(); request.setTitle("Edited while review was running"); request.setContent("New reviewed version");
                service.updateArticle(7L, 10L, request);
            });
            assertTrue(gate.editorEntered.await(5, java.util.concurrent.TimeUnit.SECONDS));
            gate.release.countDown();
            approve.get(10, java.util.concurrent.TimeUnit.SECONDS); edit.get(10, java.util.concurrent.TimeUnit.SECONDS);
            assertEquals(1, status()); assertEquals("REQUIRED", state());
            assertEquals("New reviewed version", jdbc.queryForObject("select content from cms_article where id=10", String.class));
        } finally { gate.release.countDown(); executor.shutdownNow(); }
    }

    @Test void manuallyApprovedCurrentContentMayBePublishedAfterReReview() {
        service.updateArticleStatus(99L, 10L, approval());
        var edit = new ArticleUpdateRequest(); edit.setTitle("New title"); edit.setContent("New content");
        service.updateArticle(7L, 10L, edit);
        service.updateArticleStatus(99L, 10L, approval());
        assertEquals(2, status()); assertEquals("APPROVED", state());
        assertEquals("New content", jdbc.queryForObject("select content from cms_article where id=10", String.class));
    }
}
