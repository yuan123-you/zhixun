package com.zhixun.service.impl;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhixun.common.exception.BusinessException;
import com.zhixun.common.util.SecurityUtil;
import com.zhixun.common.util.SensitiveWordUtil;
import com.zhixun.dto.article.ArticleQueryRequest;
import com.zhixun.dto.comment.CommentCreateRequest;
import com.zhixun.entity.Article;
import com.zhixun.entity.Comment;
import com.zhixun.entity.UserFollow;
import com.zhixun.mapper.*;
import com.zhixun.service.*;
import com.zhixun.vo.ArticleVO;
import java.lang.reflect.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ContentAccessRegressionTest {
    private JdbcTemplate jdbc;
    private SqlSessionTemplate sessions;
    private SecurityUtil security;
    private ValueOperations<String, String> values;
    private ArticleServiceImpl articles;
    private CommentServiceImpl comments;
    private FeedServiceImpl feeds;
    private AdminServiceImpl admin;
    private UserFollowMapper follows;
    private ZSetOperations<String, String> timelines;
    private final Map<String, String> cache = new ConcurrentHashMap<>();
    private CountDownLatch concurrentReads;
    private CountDownLatch concurrentCommentReads;
    private final boolean nativeMySql = Boolean.getBoolean("review.nativeMysql");

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() throws Exception {
        DriverManagerDataSource source;
        if (nativeMySql) {
            var props = new Properties();
            try (var in = java.nio.file.Files.newInputStream(java.nio.file.Path.of("target/mysql-fixture/mysql-test.properties"))) {
                props.load(in);
            }
            String schema = props.getProperty("schema");
            if (!"zhixun_review_fixture".equals(schema)) throw new IllegalStateException("Only the isolated review fixture is allowed");
            source = new DriverManagerDataSource("jdbc:mysql://127.0.0.1:" + props.getProperty("port") + "/" + schema
                    + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC", props.getProperty("user"), props.getProperty("password"));
        } else {
            source = new DriverManagerDataSource("jdbc:h2:mem:review_" + UUID.randomUUID()
                    + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        }
        jdbc = new JdbcTemplate(source);
        if (nativeMySql) jdbc.execute("drop table if exists cms_article,cms_comment,user_follow");
        table("cms_article", Article.class);
        table("cms_comment", Comment.class);
        table("user_follow", UserFollow.class);
        var factory = new MybatisSqlSessionFactoryBean();
        factory.setDataSource(source);
        var config = new MybatisConfiguration();
        config.setMapUnderscoreToCamelCase(true);
        config.addMapper(ArticleMapper.class);
        config.addMapper(CommentMapper.class);
        config.addMapper(UserFollowMapper.class);
        factory.setConfiguration(config);
        var pagination = new MybatisPlusInterceptor();
        pagination.addInnerInterceptor(new PaginationInnerInterceptor());
        factory.setPlugins(pagination);
        sessions = new SqlSessionTemplate(factory.getObject());
        ArticleMapper mapper = sessions.getMapper(ArticleMapper.class);
        ArticleMapper observed = (ArticleMapper) Proxy.newProxyInstance(ArticleMapper.class.getClassLoader(),
                new Class<?>[]{ArticleMapper.class}, (proxy, method, args) -> {
                    Object result;
                    try { result = method.invoke(mapper, args); }
                    catch (InvocationTargetException e) { throw e.getCause(); }
                    if (concurrentReads != null && method.getName().equals("selectById")) {
                        concurrentReads.countDown();
                        if (!concurrentReads.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("read gate timed out");
                    }
                    return result;
                });
        security = mock(SecurityUtil.class);
        var redis = mock(StringRedisTemplate.class);
        values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        timelines = mock(ZSetOperations.class);
        when(redis.opsForZSet()).thenReturn(timelines);
        when(values.get(anyString())).thenAnswer(inv -> cache.get(inv.getArgument(0)));
        doAnswer(inv -> { cache.put(inv.getArgument(0), inv.getArgument(1)); return null; })
                .when(values).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));
        follows = sessions.getMapper(UserFollowMapper.class);
        var tags = mock(ArticleTagMapper.class);
        var likes = mock(ArticleLikeMapper.class);
        var users = mock(UserMapper.class);
        var sensitive = mock(SensitiveWordUtil.class);
        articles = new ArticleServiceImpl(observed, tags, likes, mock(ArticleViewHistoryMapper.class),
                mock(CollectMapper.class), follows, mock(ArticleOperateLogMapper.class),
                users, mock(CategoryMapper.class), mock(TagMapper.class), mock(ArticleImageMapper.class),
                sensitive, security, mock(OpenSearchSyncService.class), mock(FeedService.class), redis,
                mock(RabbitTemplate.class), new ObjectMapper());
        feeds = new FeedServiceImpl(observed, tags, users, mock(CategoryMapper.class), mock(TagMapper.class),
                mock(UserPreferredCategoryMapper.class), mock(UserPreferredTagMapper.class), follows, redis,
                mock(CollaborativeFilterService.class), mock(ColdStartBanditService.class), mock(ContentBasedRecommendService.class));
        CommentMapper commentMapper = sessions.getMapper(CommentMapper.class);
        CommentMapper observedComments = (CommentMapper) Proxy.newProxyInstance(CommentMapper.class.getClassLoader(),
                new Class<?>[]{CommentMapper.class}, (proxy, method, args) -> {
                    Object result;
                    try { result = method.invoke(commentMapper, args); }
                    catch (InvocationTargetException e) { throw e.getCause(); }
                    if (concurrentCommentReads != null && method.getName().equals("selectById")) {
                        concurrentCommentReads.countDown();
                        if (!concurrentCommentReads.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("comment read gate timed out");
                    }
                    return result;
                });
        comments = new CommentServiceImpl(observedComments, observed, likes, users,
                mock(CommentReportMapper.class), sensitive, mock(NotificationService.class), security, new ObjectMapper());
        admin = new AdminServiceImpl(observed, articles, likes, tags, mock(ArticleViewHistoryMapper.class), users,
                mock(CategoryMapper.class), mock(TagMapper.class), observedComments, mock(SensitiveWordMapper.class),
                mock(SensitiveWhitelistMapper.class), mock(OperationLogMapper.class), mock(SecurityAuditLogMapper.class),
                mock(NotificationMapper.class), mock(LoginLogMapper.class), mock(ViewHistoryMapper.class), mock(GroupMapper.class),
                mock(GroupJoinRequestMapper.class), mock(GroupMemberMapper.class), mock(GroupMessageMapper.class),
                mock(ArticleCollaboratorMapper.class), mock(UserMessageMapper.class), mock(OperationLogService.class),
                sensitive, mock(NotificationService.class));
        jdbc.update("insert into cms_article(id,author_id,title,content,status,visibility,comment_count,is_top,view_count) "
                + "values(10,7,'Draft','Private draft',0,0,0,0,0),(11,8,'Public','Published',2,0,0,0,0)");
    }

    @AfterEach
    void tearDown() {
        jdbc.execute(nativeMySql ? "drop table if exists cms_article,cms_comment,user_follow" : "drop all objects");
    }

    private void table(String name, Class<?> type) {
        List<String> columns = new ArrayList<>();
        for (var field : type.getDeclaredFields()) {
            TableField annotation = field.getAnnotation(TableField.class);
            if (Modifier.isStatic(field.getModifiers()) || (annotation != null && !annotation.exist())) continue;
            String column = field.getName().replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase(Locale.ROOT);
            Class<?> property = field.getType();
            String sql = property == Long.class ? "bigint" : property == Integer.class || property.isEnum() ? "integer"
                    : property == LocalDateTime.class ? "timestamp" : property == BigDecimal.class ? "decimal(18,4)" : "varchar(4096)";
            if (nativeMySql && sql.startsWith("varchar")) sql = "text";
            String id = nativeMySql ? "id bigint auto_increment primary key" : "id bigint generated by default as identity primary key";
            columns.add(column.equals("id") ? id : column + " " + sql);
        }
        jdbc.execute("create table " + name + "(" + String.join(",", columns) + ")");
    }

    private ArticleQueryRequest query(int status) {
        var request = new ArticleQueryRequest(); request.setStatus(status); return request;
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 3, 4})
    void anonymousStatusFilterCannotExposeUnpublishedContent(int status) {
        jdbc.update("update cms_article set status=? where id=10", status);
        var result = articles.getArticleList(query(status));
        assertTrue(result.getList().isEmpty());
        assertEquals(0L, result.getTotal());
    }

    @Test
    void anotherUserCannotListSomeoneElsesDrafts() {
        when(security.getCurrentUserId()).thenReturn(8L);
        var result = articles.getArticleList(query(0));
        assertTrue(result.getList().isEmpty());
        assertEquals(0L, result.getTotal());
    }

    @Test
    void draftAuthorCanStillListTheirOwnDrafts() {
        when(security.getCurrentUserId()).thenReturn(7L);
        assertEquals(List.of(10L), articles.getArticleList(query(0)).getList().stream().map(ArticleVO::getId).toList());
    }

    @Test
    void administratorCanStillListPendingContent() {
        when(security.getCurrentUserId()).thenReturn(9L);
        when(security.isAdmin()).thenReturn(true);
        jdbc.update("update cms_article set status=1 where id=10");
        assertEquals(List.of(10L), articles.getArticleList(query(1)).getList().stream().map(ArticleVO::getId).toList());
    }

    @Test
    void defaultListStillReturnsPublishedContent() {
        assertEquals(List.of(11L), articles.getArticleList(new ArticleQueryRequest()).getList().stream().map(ArticleVO::getId).toList());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 3, 4})
    void cachedRecommendationsRecheckPublicationStatus(int status) {
        jdbc.update("update cms_article set status=? where id=10", status);
        when(values.get("article:related:11")).thenReturn("10,11");
        assertEquals(List.of(11L), articles.getRelatedArticles(11L, 6).stream().map(ArticleVO::getId).toList());
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3})
    void cachedRecommendationsCannotExposeRestrictedArticles(int visibility) {
        jdbc.update("update cms_article set status=2,visibility=? where id=10", visibility);
        when(values.get("article:related:11")).thenReturn("10,11");
        assertEquals(List.of(11L), articles.getRelatedArticles(11L, 6).stream().map(ArticleVO::getId).toList());
    }

    @Test
    void cachedRecommendationsRespectCurrentRequestLimit() {
        jdbc.update("update cms_article set status=2 where id=10");
        when(values.get("article:related:11")).thenReturn("10,11");
        assertEquals(List.of(10L), articles.getRelatedArticles(11L, 1).stream().map(ArticleVO::getId).toList());
    }

    @Test
    void cachedRecommendationsExcludeDeletedArticles() {
        jdbc.update("update cms_article set status=2,deleted_at=current_timestamp where id=10");
        when(values.get("article:related:11")).thenReturn("10,11");
        assertEquals(List.of(11L), articles.getRelatedArticles(11L, 6).stream().map(ArticleVO::getId).toList());
    }

    @Test
    void listDoesNotExposeDeletedPublishedArticles() {
        jdbc.update("update cms_article set deleted_at=current_timestamp where id=11");
        var result = articles.getArticleList(new ArticleQueryRequest());
        assertTrue(result.getList().isEmpty());
        assertEquals(0L, result.getTotal());
    }

    @Test
    void uncachedRecommendationsDoNotExposeDeletedPublishedArticles() {
        jdbc.update("update cms_article set status=2,deleted_at=current_timestamp,category_id=1 where id=10");
        jdbc.update("update cms_article set category_id=1 where id=11");
        assertTrue(articles.getRelatedArticles(11L, 6).isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"latest", "hot"})
    void freshFeedsDoNotReturnDeletedPublishedArticles(String type) {
        jdbc.update("update cms_article set deleted_at=current_timestamp where id=11");
        var result = type.equals("latest") ? feeds.getLatestFeed(null, 1, 10) : feeds.getHotFeed(null, 1, 10);
        assertTrue(result.getList().isEmpty());
        assertEquals(0L, result.getTotal());
    }

    @ParameterizedTest
    @ValueSource(strings = {"latest", "hot"})
    void cachedFeedSecondPageIsNotPaginatedTwice(String type) {
        jdbc.update("update cms_article set status=2,created_at='2026-10-01 00:00:00',view_count=1 where id=10");
        jdbc.update("update cms_article set created_at='2026-10-02 00:00:00',view_count=2 where id=11");
        var first = type.equals("latest") ? feeds.getLatestFeed(null, 2, 1) : feeds.getHotFeed(null, 2, 1);
        var cached = type.equals("latest") ? feeds.getLatestFeed(null, 2, 1) : feeds.getHotFeed(null, 2, 1);
        assertEquals(List.of(10L), first.getList().stream().map(ArticleVO::getId).toList());
        assertEquals(List.of(10L), cached.getList().stream().map(ArticleVO::getId).toList());
        assertEquals(2L, cached.getTotal());
    }

    @ParameterizedTest
    @ValueSource(strings = {"latest", "hot"})
    void cachedFeedKeepsTheDatabaseTotal(String type) {
        jdbc.update("update cms_article set status=2 where id=10");
        var first = type.equals("latest") ? feeds.getLatestFeed(null, 1, 1) : feeds.getHotFeed(null, 1, 1);
        var cached = type.equals("latest") ? feeds.getLatestFeed(null, 1, 1) : feeds.getHotFeed(null, 1, 1);
        assertEquals(2L, first.getTotal());
        assertEquals(2L, cached.getTotal());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 3, 4})
    void recommendationCacheDoesNotResurrectWithdrawnArticles(int status) {
        jdbc.update("update cms_article set status=? where id=10", status);
        cache.put("feed:recommend:key:8", "test-batch");
        cache.put("feed:recommend:test-batch", "10,11");
        assertEquals(List.of(11L), feeds.getRecommendFeed(8L, 0, 1, 10).getList().stream().map(ArticleVO::getId).toList());
    }

    @ParameterizedTest
    @ValueSource(strings = {"latest", "hot"})
    void legacyPageCachesFallBackToTheDatabase(String type) {
        jdbc.update("update cms_article set status=2,created_at='2026-10-01 00:00:00',view_count=1 where id=10");
        jdbc.update("update cms_article set created_at='2026-10-02 00:00:00',view_count=2 where id=11");
        cache.put("feed:" + type + ":2:1", "10");
        var result = type.equals("latest") ? feeds.getLatestFeed(null, 2, 1) : feeds.getHotFeed(null, 2, 1);
        assertEquals(List.of(10L), result.getList().stream().map(ArticleVO::getId).toList());
        assertEquals(2L, result.getTotal());
    }

    @Test
    void cursorFollowingFeedDoesNotExposePrivateArticles() {
        jdbc.update("insert into user_follow(follower_id,following_id) values(8,7)");
        jdbc.update("update cms_article set status=2,visibility=3 where id=10");
        assertTrue(feeds.getFollowingFeedByCursor(8L, null, 10).getList().isEmpty());
    }

    private void restrictedFirst(int visibility) {
        jdbc.update("update cms_article set status=2,visibility=?,created_at='2026-10-20 00:00:00',updated_at='2026-10-20 00:00:00',view_count=100 where id=10", visibility);
        jdbc.update("update cms_article set created_at='2026-10-19 00:00:00',updated_at='2026-10-19 00:00:00' where id=11");
    }

    @Test
    void anonymousArticlePaginationCountsAndFillsOnlyVisibleRows() {
        restrictedFirst(3);
        var query = new ArticleQueryRequest(); query.setPageSize(1);
        var result = articles.getArticleList(query);
        assertEquals(List.of(11L), result.getList().stream().map(ArticleVO::getId).toList());
        assertEquals(1L, result.getTotal());
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2})
    void articlePaginationAllowsOnlyTheRequiredFollowRelationship(int visibility) {
        restrictedFirst(visibility);
        when(security.getCurrentUserId()).thenReturn(9L);
        var query = new ArticleQueryRequest(); query.setPageSize(1);
        var hidden = articles.getArticleList(query);
        assertEquals(List.of(11L), hidden.getList().stream().map(ArticleVO::getId).toList());
        assertEquals(1L, hidden.getTotal());
        jdbc.update("insert into user_follow(follower_id,following_id) values(9,7)");
        if (visibility == 2) jdbc.update("insert into user_follow(follower_id,following_id) values(7,9)");
        var visible = articles.getArticleList(query);
        assertEquals(List.of(10L), visible.getList().stream().map(ArticleVO::getId).toList());
        assertEquals(2L, visible.getTotal());
    }

    @ParameterizedTest
    @ValueSource(strings = {"latest", "hot"})
    void feedPaginationFillsFromAuthorizedRowsBeforeApplyingLimit(String type) {
        restrictedFirst(3);
        var result = type.equals("latest") ? feeds.getLatestFeed(null, 1, 1) : feeds.getHotFeed(null, 1, 1);
        assertEquals(List.of(11L), result.getList().stream().map(ArticleVO::getId).toList());
        assertEquals(1L, result.getTotal());
    }

    @ParameterizedTest
    @ValueSource(strings = {"latest", "hot"})
    void aPrivateArticleAuthorsCacheCannotPoisonAnonymousPages(String type) {
        restrictedFirst(3);
        if (type.equals("latest")) feeds.getLatestFeed(7L, 1, 1); else feeds.getHotFeed(7L, 1, 1);
        var result = type.equals("latest") ? feeds.getLatestFeed(null, 1, 1) : feeds.getHotFeed(null, 1, 1);
        assertEquals(List.of(11L), result.getList().stream().map(ArticleVO::getId).toList());
        assertEquals(1L, result.getTotal());
    }

    @ParameterizedTest
    @ValueSource(strings = {"latest", "hot"})
    void anAnonymousPageCacheCannotHideTheAuthorsPrivateRows(String type) {
        restrictedFirst(3);
        if (type.equals("latest")) feeds.getLatestFeed(null, 1, 2); else feeds.getHotFeed(null, 1, 2);
        var result = type.equals("latest") ? feeds.getLatestFeed(7L, 1, 2) : feeds.getHotFeed(7L, 1, 2);
        assertEquals(List.of(10L, 11L), result.getList().stream().map(ArticleVO::getId).toList());
        assertEquals(2L, result.getTotal());
    }

    @Test
    void recommendedIdsAreAuthorizedBeforePaginationAndCounting() {
        restrictedFirst(3);
        cache.put("feed:recommend:key:9", "test-batch");
        cache.put("feed:recommend:test-batch", "10,11");
        var result = feeds.getRecommendFeed(9L, 0, 1, 1);
        assertEquals(List.of(11L), result.getList().stream().map(ArticleVO::getId).toList());
        assertEquals(1L, result.getTotal());
    }

    @Test
    void followingDatabasePageCountsOnlyCurrentlyVisibleRows() {
        restrictedFirst(3);
        jdbc.update("insert into user_follow(follower_id,following_id) values(9,7),(9,8)");
        var result = feeds.getFollowingFeed(9L, 1, 1);
        assertEquals(List.of(11L), result.getList().stream().map(ArticleVO::getId).toList());
        assertEquals(1L, result.getTotal());
    }

    @Test
    void followingPageOneCacheDoesNotReplaceTheWholeFollowingSequence() {
        restrictedFirst(0);
        jdbc.update("insert into user_follow(follower_id,following_id) values(9,7),(9,8)");
        var first = feeds.getFollowingFeed(9L, 1, 1);
        var second = feeds.getFollowingFeed(9L, 2, 1);
        assertEquals(List.of(10L), first.getList().stream().map(ArticleVO::getId).toList());
        assertEquals(List.of(11L), second.getList().stream().map(ArticleVO::getId).toList());
        assertEquals(2L, second.getTotal());
    }

    @Test
    void aPrivateTimelineHeadCannotHideTheNextVisibleFollowingRow() {
        restrictedFirst(3);
        jdbc.update("insert into user_follow(follower_id,following_id) values(9,7),(9,8)");
        when(timelines.size("timeline:user:9")).thenReturn(2L);
        when(timelines.reverseRange("timeline:user:9", 0L, 0L)).thenReturn(new LinkedHashSet<>(List.of("10")));
        var result = feeds.getFollowingFeed(9L, 1, 1);
        assertEquals(List.of(11L), result.getList().stream().map(ArticleVO::getId).toList());
        assertEquals(1L, result.getTotal());
    }

    @Test
    void followingCursorAppliesVisibilityBeforeLimitingTheDatabaseRows() {
        restrictedFirst(3);
        jdbc.update("insert into user_follow(follower_id,following_id) values(9,7),(9,8)");
        assertEquals(List.of(11L), feeds.getFollowingFeedByCursor(9L, null, 1).getList().stream().map(ArticleVO::getId).toList());
    }

    private CommentCreateRequest comment(Long parentId) {
        var request = new CommentCreateRequest(); request.setArticleId(11L);
        request.setContent("Reply"); request.setParentId(parentId); return request;
    }

    @Test
    void repliesCannotBeAttachedToACommentOnAnotherArticle() {
        jdbc.update("insert into cms_comment(id,article_id,user_id,parent_id,status,content) values(20,10,7,0,1,'Other article')");
        assertThrows(BusinessException.class, () -> comments.createComment(8L, comment(20L)));
        assertEquals(1, jdbc.queryForObject("select count(*) from cms_comment", Integer.class));
        assertEquals(0L, jdbc.queryForObject("select comment_count from cms_article where id=11", Long.class));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 2, 3})
    void repliesCannotBeAttachedToHiddenOrDeletedComments(int status) {
        jdbc.update("insert into cms_comment(id,article_id,user_id,parent_id,status,content) values(20,11,7,0,?,'Hidden')", status);
        assertThrows(BusinessException.class, () -> comments.createComment(8L, comment(20L)));
    }

    @Test
    void normalReplyStillIncrementsTheArticleCount() {
        jdbc.update("insert into cms_comment(id,article_id,user_id,parent_id,status,content) values(20,11,7,0,1,'Parent')");
        var reply = comments.createComment(8L, comment(20L));
        assertEquals(20L, reply.getParentId());
        assertEquals(1L, jdbc.queryForObject("select comment_count from cms_article where id=11", Long.class));
    }

    @Test
    void approvingTheSameCommentTwiceDoesNotInflateTheCounter() {
        jdbc.update("insert into cms_comment(id,article_id,user_id,parent_id,status,content) values(20,11,7,0,0,'Pending')");
        comments.auditComment(9L, 20L, "approve", null);
        comments.auditComment(9L, 20L, "approve", null);
        assertEquals(1L, jdbc.queryForObject("select comment_count from cms_article where id=11", Long.class));
        assertEquals(1, jdbc.queryForObject("select status from cms_comment where id=20", Integer.class));
    }

    @Test
    void rejectingANormalCommentRemovesItFromTheCounter() {
        jdbc.update("insert into cms_comment(id,article_id,user_id,parent_id,status,content) values(20,11,7,0,1,'Normal')");
        jdbc.update("update cms_article set comment_count=1 where id=11");
        comments.auditComment(9L, 20L, "reject", "Moderated");
        comments.auditComment(9L, 20L, "reject", "Repeated");
        assertEquals(0L, jdbc.queryForObject("select comment_count from cms_article where id=11", Long.class));
        assertEquals(2, jdbc.queryForObject("select status from cms_comment where id=20", Integer.class));
    }

    @Test
    void simultaneousApprovalCountsTheCommentOnlyOnce() throws Exception {
        jdbc.update("insert into cms_comment(id,article_id,user_id,parent_id,status,content) values(20,11,7,0,0,'Pending')");
        concurrentCommentReads = new CountDownLatch(2);
        var pool = Executors.newFixedThreadPool(2);
        try {
            var first = pool.submit(() -> comments.auditComment(9L, 20L, "approve", null));
            var second = pool.submit(() -> comments.auditComment(9L, 20L, "approve", null));
            first.get(10, TimeUnit.SECONDS); second.get(10, TimeUnit.SECONDS);
            assertEquals(1L, jdbc.queryForObject("select comment_count from cms_article where id=11", Long.class));
        } finally { pool.shutdownNow(); }
    }

    @ParameterizedTest
    @ValueSource(strings = {"user", "admin"})
    void simultaneousDeletionDoesNotDecrementForAnotherComment(String endpoint) throws Exception {
        jdbc.update("insert into cms_comment(id,article_id,user_id,parent_id,status,content) values(20,11,7,0,1,'Normal')");
        jdbc.update("update cms_article set comment_count=2 where id=11");
        when(security.isOwnerOrAdmin(7L)).thenReturn(true);
        concurrentCommentReads = new CountDownLatch(2);
        var pool = Executors.newFixedThreadPool(2);
        try {
            Runnable delete = endpoint.equals("admin") ? () -> admin.deleteCommentAsAdmin(9L, 20L)
                    : () -> comments.deleteComment(7L, 20L);
            var first = pool.submit(delete);
            var second = pool.submit(delete);
            first.get(10, TimeUnit.SECONDS); second.get(10, TimeUnit.SECONDS);
            assertEquals(1L, jdbc.queryForObject("select comment_count from cms_article where id=11", Long.class));
        } finally { pool.shutdownNow(); }
    }

    @Test
    void simultaneousCommentsDoNotLoseTheArticleCounterUpdate() throws Exception {
        concurrentReads = new CountDownLatch(2);
        var pool = Executors.newFixedThreadPool(2);
        try {
            var first = pool.submit(() -> comments.createComment(7L, comment(null)));
            var second = pool.submit(() -> comments.createComment(8L, comment(null)));
            first.get(10, TimeUnit.SECONDS); second.get(10, TimeUnit.SECONDS);
            assertEquals(2, jdbc.queryForObject("select count(*) from cms_comment", Integer.class));
            assertEquals(2L, jdbc.queryForObject("select comment_count from cms_article where id=11", Long.class));
        } finally { pool.shutdownNow(); }
    }
}
