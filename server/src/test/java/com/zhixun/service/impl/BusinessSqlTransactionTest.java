package com.zhixun.service.impl;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhixun.common.util.SecurityUtil;
import com.zhixun.common.util.SensitiveWordUtil;
import com.zhixun.entity.*;
import com.zhixun.mapper.*;
import com.zhixun.service.FeedService;
import com.zhixun.service.OpenSearchSyncService;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import javax.sql.DataSource;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.*;
import org.mybatis.spring.mapper.MapperFactoryBean;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.*;
import org.springframework.data.redis.core.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class BusinessSqlTransactionTest {
    static class ReadHook { Runnable afterRead = () -> {}; boolean failReload, missingReload; }
    @Configuration @EnableTransactionManagement(proxyTargetClass = true)
    static class Config {
        @Bean DataSource dataSource() { return new DriverManagerDataSource("jdbc:h2:mem:zhixun_" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", ""); }
        @Bean JdbcTemplate jdbc(DataSource source) { return new JdbcTemplate(source); }
        @Bean PlatformTransactionManager transactionManager(DataSource source) { return new DataSourceTransactionManager(source); }
        @Bean SqlSessionFactory sqlSessionFactory(DataSource source) throws Exception {
            var factory = new MybatisSqlSessionFactoryBean(); factory.setDataSource(source);
            var config = new MybatisConfiguration(); config.setMapUnderscoreToCamelCase(true); factory.setConfiguration(config);
            return factory.getObject();
        }
        @Bean MapperFactoryBean<ArticleMapper> articles(SqlSessionFactory factory) {
            var mapper = new MapperFactoryBean<>(ArticleMapper.class); mapper.setSqlSessionFactory(factory); return mapper;
        }
        @Bean MapperFactoryBean<GroupMessageMapper> messages(SqlSessionFactory factory) {
            var mapper = new MapperFactoryBean<>(GroupMessageMapper.class); mapper.setSqlSessionFactory(factory); return mapper;
        }
        @Bean ReadHook hook() { return new ReadHook(); }
        @Bean OpenSearchSyncService search() { return mock(OpenSearchSyncService.class); }
        @Bean FeedService feed() { return mock(FeedService.class); }
        @Bean RabbitTemplate broker() { return mock(RabbitTemplate.class); }
        @Bean StringRedisTemplate redis() {
            var redis = mock(StringRedisTemplate.class);
            @SuppressWarnings("unchecked") Cursor<String> cursor = mock(Cursor.class);
            when(redis.scan(any())).thenReturn(cursor);
            @SuppressWarnings("unchecked") ValueOperations<String,String> values = mock(ValueOperations.class);
            when(redis.opsForValue()).thenReturn(values); when(values.increment(anyString())).thenReturn(1L); return redis;
        }
        @Bean ArticleServiceImpl service(ArticleMapper articles, ReadHook hook, OpenSearchSyncService search,
                                        FeedService feed, StringRedisTemplate redis, RabbitTemplate broker) {
            var observed = (ArticleMapper) Proxy.newProxyInstance(ArticleMapper.class.getClassLoader(), new Class<?>[]{ArticleMapper.class},
                    (proxy, method, args) -> {
                        Object result;
                        try { result = method.invoke(articles, args); } catch (InvocationTargetException e) { throw e.getCause(); }
                        if (method.getName().equals("selectList")) hook.afterRead.run();
                        if (method.getName().equals("selectById") && hook.failReload) throw new IllegalStateException("metadata reload failed");
                        if (method.getName().equals("selectById") && hook.missingReload) return null;
                        return result;
                    });
            return new ArticleServiceImpl(observed, mock(ArticleTagMapper.class), mock(ArticleLikeMapper.class),
                    mock(ArticleViewHistoryMapper.class), mock(CollectMapper.class), mock(UserFollowMapper.class),
                    mock(ArticleOperateLogMapper.class), mock(UserMapper.class), mock(CategoryMapper.class), mock(TagMapper.class),
                    mock(ArticleImageMapper.class), mock(SensitiveWordUtil.class), mock(SecurityUtil.class),
                    search, feed, redis, broker, new ObjectMapper());
        }
    }

    private AnnotationConfigApplicationContext context;
    private JdbcTemplate jdbc;
    private ArticleServiceImpl service;
    private GroupMessageMapper messages;
    private OpenSearchSyncService search;
    private FeedService feed;
    private StringRedisTemplate redis;

    @BeforeEach void setUp() {
        context = new AnnotationConfigApplicationContext(Config.class);
        jdbc = context.getBean(JdbcTemplate.class); service = context.getBean(ArticleServiceImpl.class);
        messages = context.getBean(GroupMessageMapper.class); search = context.getBean(OpenSearchSyncService.class);
        feed = context.getBean(FeedService.class); redis = context.getBean(StringRedisTemplate.class);
        table("cms_article", Article.class); table("cms_group_info", GroupInfo.class);
        table("cms_group_member", GroupMember.class); table("cms_group_message", GroupMessage.class);
        jdbc.update("insert into cms_article(id,author_id,title,content,status,publish_at,review_state) values(10,7,'Original','Content',1,?,'CLEARED')", LocalDateTime.now().minusMinutes(5));
        jdbc.update("insert into cms_group_info(id,status) values(2,0)");
        jdbc.update("insert into cms_group_member(group_id,user_id) values(2,7)");
        clearInvocations(search, feed, redis); // Exclude Spring bean initialization, not business effects.
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
            columns.add(column.equals("id") ? "id bigint generated by default as identity primary key" : column + " " + sql);
        }
        jdbc.execute("create table " + name + "(" + String.join(",", columns) + ")");
    }
    @AfterEach void tearDown() { jdbc.execute("drop all objects"); context.close(); }
    private TransactionTemplate tx() { return new TransactionTemplate(context.getBean(PlatformTransactionManager.class)); }
    private void concurrentChange(String sql, Object... args) {
        var tx = tx(); tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        tx.executeWithoutResult(status -> jdbc.update(sql, args));
    }
    private GroupMessage message() {
        GroupMessage message = new GroupMessage(); message.setGroupId(2L); message.setContent("Valid final");
        message.setCreatedAt(LocalDateTime.now()); return message;
    }

    @Test void validGroupReplyHasAnActualGeneratedIdAndSavedContent() {
        GroupMessage message = message();
        assertEquals(1, messages.insertAiIfActive(message, 7L)); assertNotNull(message.getId());
        assertEquals("Valid final", jdbc.queryForObject("select content from cms_group_message where id=?", String.class, message.getId()));
    }
    @Test void revokedMembershipPreventsConditionalReplyInsert() {
        jdbc.update("delete from cms_group_member where group_id=2 and user_id=7");
        assertEquals(0, messages.insertAiIfActive(message(), 7L));
        assertEquals(0, jdbc.queryForObject("select count(*) from cms_group_message", Integer.class));
    }
    @Test void dismissedGroupPreventsConditionalReplyInsert() {
        jdbc.update("update cms_group_info set status=1 where id=2");
        assertEquals(0, messages.insertAiIfActive(message(), 7L));
        assertEquals(0, jdbc.queryForObject("select count(*) from cms_group_message", Integer.class));
    }
    @Test void anotherUserCannotBorrowRequestersMembership() {
        assertEquals(0, messages.insertAiIfActive(message(), 8L));
        assertEquals(0, jdbc.queryForObject("select count(*) from cms_group_message", Integer.class));
    }

    @Test void publicationEffectsWaitUntilOuterDatabaseCommit() {
        tx().executeWithoutResult(status -> {
            service.publishScheduledArticles(); verifyNoInteractions(search, feed, redis);
        });
        assertEquals(2, jdbc.queryForObject("select status from cms_article where id=10", Integer.class));
        verify(search).syncArticle(10L); verify(feed).fanoutOnPublish(eq(7L), eq(10L), any());
    }
    @Test void outerRollbackLeavesArticlePendingAndNoEscapedEffects() {
        assertThrows(IllegalStateException.class, () -> tx().executeWithoutResult(status -> {
            service.publishScheduledArticles(); throw new IllegalStateException("outer rollback");
        }));
        assertEquals(1, jdbc.queryForObject("select status from cms_article where id=10", Integer.class));
        verifyNoInteractions(search, feed, redis);
    }
    @Test void staleSchedulingCannotOverwriteConcurrentRejection() {
        context.getBean(ReadHook.class).afterRead = () -> concurrentChange("update cms_article set status=3 where id=10");
        service.publishScheduledArticles();
        assertEquals(3, jdbc.queryForObject("select status from cms_article where id=10", Integer.class));
        verifyNoInteractions(search, feed, redis);
    }
    @Test void staleSchedulingCannotOverwriteANewFuturePublishDate() {
        LocalDateTime future = LocalDateTime.now().plusDays(3).withNano(0);
        context.getBean(ReadHook.class).afterRead = () -> concurrentChange("update cms_article set publish_at=? where id=10", future);
        service.publishScheduledArticles();
        assertEquals(1, jdbc.queryForObject("select status from cms_article where id=10", Integer.class));
        assertEquals(future, jdbc.queryForObject("select publish_at from cms_article where id=10", java.sql.Timestamp.class).toLocalDateTime());
        verifyNoInteractions(search, feed, redis);
    }
    @Test void concurrentDeletionCannotBeResurrectedByScheduler() {
        context.getBean(ReadHook.class).afterRead = () -> concurrentChange("update cms_article set deleted_at=? where id=10", LocalDateTime.now());
        service.publishScheduledArticles();
        assertEquals(1, jdbc.queryForObject("select status from cms_article where id=10", Integer.class));
        assertNotNull(jdbc.queryForObject("select deleted_at from cms_article where id=10", java.sql.Timestamp.class));
        verifyNoInteractions(search, feed, redis);
    }
    @Test void failedRequiredMetadataReloadRollsBackScheduledPublication() {
        context.getBean(ReadHook.class).failReload = true;
        assertThrows(RuntimeException.class, () -> service.publishScheduledArticles());
        assertEquals(1, jdbc.queryForObject("select status from cms_article where id=10", Integer.class));
        verifyNoInteractions(search, feed, redis);
    }
    @Test void missingRequiredMetadataCannotSilentlyCommitPublishedState() {
        context.getBean(ReadHook.class).missingReload = true;
        assertThrows(RuntimeException.class, () -> service.publishScheduledArticles());
        assertEquals(1, jdbc.queryForObject("select status from cms_article where id=10", Integer.class));
        verifyNoInteractions(search, feed, redis);
    }
    @Test void followerFeedUsesTheLatestStillDuePublicationTimestamp() {
        LocalDateTime latest = LocalDateTime.now().minusMinutes(2).withNano(0);
        context.getBean(ReadHook.class).afterRead = () -> concurrentChange("update cms_article set publish_at=? where id=10", latest);
        service.publishScheduledArticles();
        assertEquals(2, jdbc.queryForObject("select status from cms_article where id=10", Integer.class));
        verify(feed).fanoutOnPublish(7L, 10L, latest);
    }

    @Test void scheduledStatusUpdateDoesNotOverwriteNewerContent() {
        context.getBean(ReadHook.class).afterRead = () -> concurrentChange("update cms_article set title='Newer content' where id=10");
        service.publishScheduledArticles();
        assertEquals(2, jdbc.queryForObject("select status from cms_article where id=10", Integer.class));
        assertEquals("Newer content", jdbc.queryForObject("select title from cms_article where id=10", String.class));
    }
}
