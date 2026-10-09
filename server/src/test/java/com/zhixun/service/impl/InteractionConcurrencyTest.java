package com.zhixun.service.impl;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.zhixun.common.exception.BusinessException;
import com.zhixun.common.util.SecurityUtil;
import com.zhixun.entity.*;
import com.zhixun.mapper.*;
import java.lang.reflect.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import javax.sql.DataSource;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mybatis.spring.mapper.MapperFactoryBean;
import org.springframework.context.annotation.*;
import org.springframework.data.redis.core.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** No application context, real DB, Redis connection, or external AI services. */
class InteractionConcurrencyTest {
    static class Hook {
        volatile String zeroMethod, failMethod;
        volatile CyclicBarrier legacyReadBarrier;
        <T> T observe(Class<T> type, T mapper) {
            return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (proxy, method, args) -> {
                String key = type.getSimpleName() + "." + method.getName();
                if (key.equals(zeroMethod)) return 0;
                if (key.equals(failMethod)) throw new IllegalStateException("injected mapper failure");
                Object result;
                try { result = method.invoke(mapper, args); } catch (InvocationTargetException e) { throw e.getCause(); }
                // Forces the old unlocked read/read race; new locking fact methods never use this path.
                if (method.getName().equals("selectOne") && legacyReadBarrier != null) legacyReadBarrier.await(5, TimeUnit.SECONDS);
                return result;
            }));
        }
    }
    @Configuration @EnableTransactionManagement(proxyTargetClass = true)
    static class Config {
        @Bean DataSource source() { return new DriverManagerDataSource("jdbc:h2:mem:interaction_" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000", "sa", ""); }
        @Bean JdbcTemplate jdbc(DataSource source) { return new JdbcTemplate(source); }
        @Bean PlatformTransactionManager transactionManager(DataSource source) { return new DataSourceTransactionManager(source); }
        @Bean SqlSessionFactory sqlSessionFactory(DataSource source) throws Exception {
            var bean = new MybatisSqlSessionFactoryBean(); bean.setDataSource(source);
            var config = new MybatisConfiguration(); config.setMapUnderscoreToCamelCase(true); bean.setConfiguration(config); return bean.getObject();
        }
        private <T> T mapper(Class<T> type, SqlSessionFactory factory, Hook hook) throws Exception {
            var bean = new MapperFactoryBean<T>(type); bean.setSqlSessionFactory(factory); bean.afterPropertiesSet();
            return hook.observe(type, bean.getObject());
        }
        @Bean Hook hook() { return new Hook(); }
        @Bean ArticleMapper articles(SqlSessionFactory factory, Hook hook) throws Exception { return mapper(ArticleMapper.class, factory, hook); }
        @Bean ArticleLikeMapper likes(SqlSessionFactory factory, Hook hook) throws Exception { return mapper(ArticleLikeMapper.class, factory, hook); }
        @Bean CollectMapper collects(SqlSessionFactory factory, Hook hook) throws Exception { return mapper(CollectMapper.class, factory, hook); }
        @Bean CommentMapper comments(SqlSessionFactory factory, Hook hook) throws Exception { return mapper(CommentMapper.class, factory, hook); }
        @Bean SecurityUtil security() { return mock(SecurityUtil.class); }
        @Bean StringRedisTemplate redis() {
            var redis = mock(StringRedisTemplate.class);
            @SuppressWarnings("unchecked") ValueOperations<String,String> values = mock(ValueOperations.class);
            when(redis.opsForValue()).thenReturn(values); when(values.increment(anyString())).thenReturn(1L);
            when(redis.scan(any())).thenAnswer(invocation -> mock(Cursor.class)); return redis;
        }
        @Bean LikeServiceImpl likeService(ArticleLikeMapper likes, ArticleMapper articles, CommentMapper comments, StringRedisTemplate redis, SecurityUtil security) {
            return new LikeServiceImpl(likes, articles, security, comments, mock(ArticleTagMapper.class), mock(TagMapper.class), mock(UserMapper.class), mock(CategoryMapper.class), redis);
        }
        @Bean CollectServiceImpl collectService(CollectMapper collects, ArticleMapper articles, StringRedisTemplate redis, SecurityUtil security) {
            var service = new CollectServiceImpl(collects, articles, security, mock(ArticleTagMapper.class), mock(TagMapper.class), mock(UserMapper.class), mock(CategoryMapper.class));
            ReflectionTestUtils.setField(service, "stringRedisTemplate", redis); return service;
        }
    }
    AnnotationConfigApplicationContext context;
    JdbcTemplate jdbc; LikeServiceImpl likes; CollectServiceImpl collects; StringRedisTemplate redis; Hook hook;
    private boolean isMySql() {
        try {
            return "MySQL".equalsIgnoreCase(context.getBean(DataSource.class).getConnection().getMetaData().getDatabaseProductName());
        } catch (Exception e) {
            return false;
        }
    }

    @BeforeEach void setup() {
        context = new AnnotationConfigApplicationContext(Config.class);
        jdbc = context.getBean(JdbcTemplate.class); likes = context.getBean(LikeServiceImpl.class);
        collects = context.getBean(CollectServiceImpl.class); redis = context.getBean(StringRedisTemplate.class); hook = context.getBean(Hook.class);
        table("cms_article", Article.class); table("cms_comment", Comment.class); table("cms_like", ArticleLike.class); table("cms_collect", Collect.class); table("user_follow", UserFollow.class);
        jdbc.execute("create unique index like_identity on cms_like(user_id,target_id,target_type)");
        jdbc.execute("create unique index collect_identity on cms_collect(user_id,article_id)");
        jdbc.update("insert into cms_article(id,author_id,status,visibility,review_state,like_count,collect_count) values(10,7,2,0,'APPROVED',0,0)");
        jdbc.update("insert into cms_comment(id,article_id,user_id,status,like_count) values(20,10,7,1,0)");
        clearInvocations(redis, redis.opsForValue());
    }
    void table(String name, Class<?> type) {
        List<String> columns = new ArrayList<>();
        for (var field : type.getDeclaredFields()) {
            var annotation = field.getAnnotation(TableField.class);
            if (Modifier.isStatic(field.getModifiers()) || annotation != null && !annotation.exist()) continue;
            String column = field.getName().replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase(Locale.ROOT);
            Class<?> property = field.getType();
            String sql = property == Long.class ? "bigint" : property == Integer.class || property.isEnum() ? "integer"
                    : property == LocalDateTime.class ? "timestamp" : property == BigDecimal.class ? "decimal(18,4)" : "varchar(4096)";
            columns.add(column.equals("id") ? "id bigint generated by default as identity primary key" : column + " " + sql);
        }
        jdbc.execute("create table " + name + "(" + String.join(",", columns) + ")");
    }
    @AfterEach void teardown() { jdbc.execute("drop all objects"); context.close(); }
    TransactionTemplate tx() { return new TransactionTemplate(context.getBean(PlatformTransactionManager.class)); }
    Map<String,Object> toggle(String kind, long user) {
        return switch (kind) { case "collect" -> collects.toggleCollect(user, 10L, "default"); case "comment" -> likes.toggleLike(user, 20L, 2); default -> likes.toggleLike(user, 10L, 1); };
    }
    long facts(String kind) { return jdbc.queryForObject(kind.equals("collect") ? "select count(*) from cms_collect" : "select count(*) from cms_like", Long.class); }
    long counter(String kind) { return jdbc.queryForObject(kind.equals("comment") ? "select like_count from cms_comment where id=20" : "select " + (kind.equals("collect") ? "collect_count" : "like_count") + " from cms_article where id=10", Long.class); }
    void concurrent(String kind, long... users) throws Exception {
        var executor = Executors.newFixedThreadPool(users.length); var start = new CountDownLatch(1);
        List<Future<Map<String,Object>>> results = new ArrayList<>();
        try {
            for (long user : users) results.add(executor.submit(() -> { assertTrue(start.await(5, TimeUnit.SECONDS)); return toggle(kind, user); }));
            start.countDown();
            for (var result : results) assertNotNull(result.get(15, TimeUnit.SECONDS));
        } finally { executor.shutdownNow(); assertTrue(executor.awaitTermination(15, TimeUnit.SECONDS)); }
    }
    @Test void invalidInputsAreBusinessErrorsBeforeAnyDatabaseOrCacheWork() {
        for (Integer type : Arrays.asList(null, -1, 0, 3, Integer.MAX_VALUE)) assertThrows(BusinessException.class, () -> likes.toggleLike(7L,10L,type));
        for (Long id : Arrays.asList(null, -1L, 0L)) {
            assertThrows(BusinessException.class, () -> likes.toggleLike(7L,id,1));
            assertThrows(BusinessException.class, () -> likes.toggleLike(id,10L,1));
            assertThrows(BusinessException.class, () -> collects.toggleCollect(7L,id,null));
            assertThrows(BusinessException.class, () -> collects.toggleCollect(id,10L,null));
        }
        assertEquals(0, facts("like")); assertEquals(0, facts("collect")); verifyNoInteractions(redis);
    }
    @ParameterizedTest @ValueSource(strings={"like","collect","comment"})
    void simultaneousSameUserTogglesFromAbsentAreTwoRealToggles(String kind) throws Exception {
        hook.legacyReadBarrier = new CyclicBarrier(2);
        concurrent(kind, 7,7); assertEquals(0,facts(kind)); assertEquals(0,counter(kind));
    }
    @ParameterizedTest @ValueSource(strings={"like","collect","comment"})
    void simultaneousSameUserTogglesFromPresentAreTwoRealToggles(String kind) throws Exception {
        toggle(kind,7); hook.legacyReadBarrier = new CyclicBarrier(2);
        concurrent(kind,7,7); assertEquals(1,facts(kind)); assertEquals(1,counter(kind));
    }
    @ParameterizedTest @ValueSource(strings={"like","collect","comment"})
    void differentUsersKeepCounterEqualToFacts(String kind) throws Exception {
        concurrent(kind,7,8,9,11,12,13); assertEquals(6,facts(kind)); assertEquals(6,counter(kind));
        concurrent(kind,7,8,9,11,12,13); assertEquals(0,facts(kind)); assertEquals(0,counter(kind));
    }
    @ParameterizedTest @ValueSource(strings={"like","collect","comment"})
    void outerRollbackKeepsFactsCountersAndRedisUntouched(String kind) {
        tx().executeWithoutResult(status -> { toggle(kind,7); verifyNoInteractions(redis); status.setRollbackOnly(); });
        assertEquals(0,facts(kind)); assertEquals(0,counter(kind)); verifyNoInteractions(redis);
    }
    @ParameterizedTest @ValueSource(strings={"like","collect","comment"})
    void effectsWaitForOuterCommit(String kind) {
        tx().executeWithoutResult(status -> { toggle(kind,7); verifyNoInteractions(redis); });
        assertEquals(1,facts(kind)); assertEquals(1,counter(kind)); verify(redis, atLeastOnce()).delete(anyString());
    }
    @ParameterizedTest @ValueSource(strings={"like","collect","comment"})
    void zeroFactInsertRollsBackAndHasNoEffects(String kind) {
        hook.zeroMethod = kind.equals("collect") ? "CollectMapper.insert" : "ArticleLikeMapper.insert";
        assertThrows(BusinessException.class, () -> toggle(kind,7)); assertEquals(0,facts(kind)); assertEquals(0,counter(kind)); verifyNoInteractions(redis);
    }
    @ParameterizedTest @ValueSource(strings={"like","collect","comment"})
    void zeroFactDeleteKeepsExistingFactAndCounter(String kind) {
        toggle(kind,7); clearInvocations(redis,redis.opsForValue());
        hook.zeroMethod = kind.equals("collect") ? "CollectMapper.deleteById" : "ArticleLikeMapper.deleteById";
        assertThrows(BusinessException.class, () -> toggle(kind,7)); assertEquals(1,facts(kind)); assertEquals(1,counter(kind)); verifyNoInteractions(redis);
    }
    @ParameterizedTest @ValueSource(strings={"like","collect","comment"})
    void throwingFactWriteRollsBack(String kind) {
        hook.failMethod = kind.equals("collect") ? "CollectMapper.insert" : "ArticleLikeMapper.insert";
        assertThrows(IllegalStateException.class, () -> toggle(kind,7)); assertEquals(0,facts(kind)); assertEquals(0,counter(kind)); verifyNoInteractions(redis);
    }
    @ParameterizedTest @ValueSource(strings={"like","collect","comment"})
    void staleNegativeCounterIsReconciledFromFacts(String kind) {
        jdbc.update(kind.equals("comment") ? "update cms_comment set like_count=-5 where id=20" : "update cms_article set like_count=-5,collect_count=-5 where id=10");
        toggle(kind,7); assertEquals(1,facts(kind)); assertEquals(1,counter(kind));
        toggle(kind,7); assertEquals(0,facts(kind)); assertEquals(0,counter(kind));
    }
    @ParameterizedTest @ValueSource(strings={"like","collect","comment"})
    void archivedAndPrivateTargetsUseExistingAuthorVisibilityRules(String kind) {
        jdbc.update("update cms_article set status=4,visibility=3 where id=10");
        assertThrows(BusinessException.class, () -> toggle(kind,8));
        toggle(kind,7); assertEquals(1,facts(kind));
    }
    @ParameterizedTest @ValueSource(strings={"like","collect","comment"})
    void deletedArticleAndCommentAreNotInteractable(String kind) {
        jdbc.update("update cms_article set deleted_at=current_timestamp where id=10");
        assertThrows(BusinessException.class, () -> toggle(kind,7)); assertEquals(0,facts(kind));
    }

    String counterMethod(String kind) {
        return kind.equals("collect") ? "ArticleMapper.saveCollectCount" : kind.equals("comment") ? "CommentMapper.saveLikeCount" : "ArticleMapper.saveLikeCount";
    }
    @ParameterizedTest @ValueSource(strings={"like","collect","comment"})
    void zeroCounterWriteRollsBackTheSuccessfulFactInsert(String kind) {
        hook.zeroMethod = counterMethod(kind);
        assertThrows(BusinessException.class, () -> toggle(kind,7));
        assertEquals(0,facts(kind)); assertEquals(0,counter(kind)); verifyNoInteractions(redis);
    }
    @ParameterizedTest @ValueSource(strings={"like","collect","comment"})
    void zeroCounterWriteRollsBackTheSuccessfulFactDelete(String kind) {
        toggle(kind,7); clearInvocations(redis,redis.opsForValue()); hook.zeroMethod = counterMethod(kind);
        assertThrows(BusinessException.class, () -> toggle(kind,7));
        assertEquals(1,facts(kind)); assertEquals(1,counter(kind)); verifyNoInteractions(redis);
    }
    @ParameterizedTest @ValueSource(strings={"like","collect","comment"})
    void throwingCounterWriteRollsBackFactInsert(String kind) {
        hook.failMethod = counterMethod(kind);
        assertThrows(IllegalStateException.class, () -> toggle(kind,7));
        assertEquals(0,facts(kind)); assertEquals(0,counter(kind)); verifyNoInteractions(redis);
    }
    @ParameterizedTest @ValueSource(strings={"like","collect","comment"})
    void throwingFactDeleteKeepsFactsAndCounter(String kind) {
        toggle(kind,7); clearInvocations(redis,redis.opsForValue());
        hook.failMethod = kind.equals("collect") ? "CollectMapper.deleteById" : "ArticleLikeMapper.deleteById";
        assertThrows(IllegalStateException.class, () -> toggle(kind,7));
        assertEquals(1,facts(kind)); assertEquals(1,counter(kind)); verifyNoInteractions(redis);
    }
    @ParameterizedTest @ValueSource(strings={"like","collect","comment"})
    void responseIsTheResultOfEachSerializedToggleAndDoesNotAlterReviewState(String kind) {
        String stateKey = kind.equals("collect") ? "isCollected" : "isLiked";
        String countKey = kind.equals("collect") ? "collectCount" : "likeCount";
        assertEquals(Map.of(stateKey,true,countKey,1L),toggle(kind,7));
        assertEquals(Map.of(stateKey,false,countKey,0L),toggle(kind,7));
        assertEquals(2,jdbc.queryForObject("select status from cms_article where id=10",Integer.class));
        assertEquals("APPROVED",jdbc.queryForObject("select review_state from cms_article where id=10",String.class));
    }
    @ParameterizedTest @ValueSource(strings={"like","collect","comment"})
    void statusCacheFailureDoesNotSkipOtherPostCommitEffects(String kind) {
        String key = kind.equals("collect") ? "collect:status:7:10" : "like:status:7:" + (kind.equals("comment") ? "20:2" : "10:1");
        doThrow(new IllegalStateException("redis unavailable")).when(redis).delete(key);
        assertDoesNotThrow(() -> toggle(kind,7)); assertEquals(1,facts(kind)); assertEquals(1,counter(kind));
        if (!kind.equals("collect")) verify(redis).delete(kind.equals("comment") ? "like:count:20:2" : "like:count:10:1");
        if (!kind.equals("comment")) {
            verify(redis).delete("article:detail:10"); verify(redis).delete("article:related:10");
            verify(redis,times(4)).scan(any()); verify(redis.opsForValue()).increment("data:version");
        }
    }
    @ParameterizedTest @ValueSource(strings={"like","collect"})
    void detailFailureAndScanFailureDoNotSkipRelatedOtherScansOrVersion(String kind) {
        doThrow(new IllegalStateException("detail unavailable")).when(redis).delete("article:detail:10");
        when(redis.scan(any())).thenThrow(new IllegalStateException("scan unavailable"));
        assertDoesNotThrow(() -> toggle(kind,7)); assertEquals(1,facts(kind)); assertEquals(1,counter(kind));
        verify(redis).delete("article:related:10"); verify(redis,times(4)).scan(any());
        verify(redis.opsForValue()).increment("data:version");
    }
    @ParameterizedTest @ValueSource(strings={"like","collect","comment"})
    void followersAndMutualFollowAccessMatchArticleDetailRules(String kind) {
        jdbc.update("update cms_article set visibility=1 where id=10");
        assertThrows(BusinessException.class, () -> toggle(kind,8));
        jdbc.update("insert into user_follow(follower_id,following_id) values(8,7)");
        toggle(kind,8); assertEquals(1,facts(kind));
        jdbc.update("update cms_article set visibility=2 where id=10");
        assertThrows(BusinessException.class, () -> toggle(kind,8)); assertEquals(1,facts(kind));
        jdbc.update("insert into user_follow(follower_id,following_id) values(7,8)");
        toggle(kind,8); assertEquals(0,facts(kind));
    }
    @ParameterizedTest @ValueSource(strings={"like","collect","comment"})
    void unpublishedAccessIsOwnerOrAdminOnlyForAllExistingStatuses(String kind) {
        for (int status : new int[]{0,1,3,4}) {
            jdbc.update("update cms_article set status=?,visibility=3 where id=10",status);
            assertThrows(BusinessException.class, () -> toggle(kind,8));
            toggle(kind,7); toggle(kind,7);
            when(context.getBean(SecurityUtil.class).isAdmin()).thenReturn(true);
            toggle(kind,8); toggle(kind,8);
            when(context.getBean(SecurityUtil.class).isAdmin()).thenReturn(false);
            assertEquals(status,jdbc.queryForObject("select status from cms_article where id=10",Integer.class));
            assertEquals("APPROVED",jdbc.queryForObject("select review_state from cms_article where id=10",String.class));
        }
    }
    @Test void deletedCommentCannotBeLikedEvenByAuthor() {
        jdbc.update("update cms_comment set deleted_at=current_timestamp where id=20");
        assertThrows(BusinessException.class, () -> toggle("comment",7)); assertEquals(0,facts("comment")); verifyNoInteractions(redis);
    }
    @Test void missingTargetsAndInvalidReadTypesAreBusinessErrors() {
        assertThrows(BusinessException.class, () -> likes.toggleLike(7L,999L,1));
        assertThrows(BusinessException.class, () -> likes.toggleLike(7L,999L,2));
        assertThrows(BusinessException.class, () -> collects.toggleCollect(7L,999L,null));
        for (Integer type : Arrays.asList(null,-1,0,3)) assertThrows(BusinessException.class, () -> likes.isLiked(7L,10L,type));
        verifyNoInteractions(redis);
    }
    @Test void optionalCollectRedisCanBeAbsent() {
        Object target = org.springframework.test.util.AopTestUtils.getTargetObject(collects);
        ReflectionTestUtils.setField(target, "stringRedisTemplate", null);
        assertEquals(Map.of("isCollected",true,"collectCount",1L),toggle("collect",7)); verifyNoInteractions(redis);
    }

    private long scalarFactCount(String kind) throws ReflectiveOperationException {
        Object mapper = kind.equals("collect") ? context.getBean(CollectMapper.class) : context.getBean(ArticleLikeMapper.class);
        Class<?> type = kind.equals("collect") ? CollectMapper.class : ArticleLikeMapper.class;
        Class<?>[] params = kind.equals("collect") ? new Class<?>[]{Long.class} : new Class<?>[]{Long.class,Integer.class};
        Method count = Arrays.stream(type.getMethods()).filter(m -> m.getName().equals("countTargetFactsForUpdate"))
                .findFirst().orElseThrow(() -> new AssertionError("Target reconciliation must return a scalar database count, not all fact IDs"));
        assertArrayEquals(params,count.getParameterTypes());
        assertEquals(long.class,count.getReturnType(),"Exactly one scalar must cross the mapper boundary");
        return (Long) (kind.equals("collect") ? count.invoke(mapper,10L) : count.invoke(mapper,kind.equals("comment") ? 20L : 10L,kind.equals("comment") ? 2 : 1));
    }
    @ParameterizedTest @ValueSource(strings={"like","collect","comment"})
    void reconciliationReturnsOneScalarForBoundedPopularTargetFixture(String kind) {
        // Bounded regression fixture, not an interaction cap or a benchmark.
        for (int user=100;user<356;user++) {
            if (kind.equals("collect")) jdbc.update("insert into cms_collect(user_id,article_id) values(?,10)",user);
            else jdbc.update("insert into cms_like(user_id,target_id,target_type) values(?,?,?)",user,kind.equals("comment") ? 20 : 10,kind.equals("comment") ? 2 : 1);
        }
        // Same table, different target: the scalar must be restricted to the authoritative target.
        if (kind.equals("collect")) jdbc.update("insert into cms_collect(user_id,article_id) values(500,999)");
        else jdbc.update("insert into cms_like(user_id,target_id,target_type) values(500,999,1)");
        tx().executeWithoutResult(status -> assertDoesNotThrow(() -> assertEquals(256,scalarFactCount(kind))));
        String countKey = kind.equals("collect") ? "collectCount" : "likeCount";
        assertEquals(257L,toggle(kind,7).get(countKey)); assertEquals(257,counter(kind));
        assertEquals(256L,toggle(kind,7).get(countKey)); assertEquals(256,counter(kind));
    }
    @ParameterizedTest @ValueSource(strings={"like","collect","comment"})
    void h2RepeatableReadRejectsChangedFactRatherThanPublishingStaleCount(String kind) throws Exception {
        Assumptions.assumeFalse(isMySql(), "H2-specific serialization failure test; MySQL InnoDB handles row locking and current reads without 40001 failure here");
        toggle(kind,7);
        var outer = tx(); outer.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
        var executor = Executors.newSingleThreadExecutor();
        try {
            var failure = assertThrows(org.springframework.dao.CannotAcquireLockException.class, () ->
                outer.executeWithoutResult(status -> {
                    var outerConnection = org.springframework.jdbc.datasource.DataSourceUtils.getConnection(context.getBean(DataSource.class));
                    assertDoesNotThrow(() -> assertEquals(java.sql.Connection.TRANSACTION_REPEATABLE_READ,outerConnection.getTransactionIsolation()));
                    assertEquals(1,facts(kind)); // Real RR snapshot on the fact table, not a simulated mapper response.
                    assertDoesNotThrow(() -> executor.submit(() -> {
                        tx().executeWithoutResult(other -> {
                            var otherConnection = org.springframework.jdbc.datasource.DataSourceUtils.getConnection(context.getBean(DataSource.class));
                            assertNotSame(outerConnection,otherConnection);
                            assertEquals(false,toggle(kind,7).get(kind.equals("collect") ? "isCollected" : "isLiked"));
                        });
                    }).get(15,TimeUnit.SECONDS)); // Completion establishes ordering after the other COMMIT.
                    clearInvocations(redis,redis.opsForValue());
                    assertEquals(1,facts(kind),"Negative control: plain COUNT reads the stale RR fact snapshot");
                    try { scalarFactCount(kind); }
                    catch (InvocationTargetException e) {
                        if (e.getCause() instanceof RuntimeException cause) throw cause;
                        throw new AssertionError(e.getCause());
                    } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
                    fail("H2 2.2.224 rejects locking a row changed since its RR snapshot; this is not a MySQL success test");
                }));
            Throwable sqlCause=failure;
            while (!(sqlCause instanceof java.sql.SQLException) && sqlCause.getCause()!=null) sqlCause=sqlCause.getCause();
            assertInstanceOf(java.sql.SQLException.class,sqlCause);
            assertEquals("40001",((java.sql.SQLException)sqlCause).getSQLState());
            assertEquals(0,facts(kind)); assertEquals(0,counter(kind)); verifyNoInteractions(redis);
            // Fresh transaction remains usable and exact after the stale-snapshot transaction rolls back.
            assertEquals(1L,toggle(kind,8).get(kind.equals("collect") ? "collectCount" : "likeCount"));
            assertEquals(1,facts(kind)); assertEquals(1,counter(kind));
        } finally { executor.shutdownNow(); assertTrue(executor.awaitTermination(15,TimeUnit.SECONDS)); }
    }
    @ParameterizedTest @ValueSource(strings={"like","collect","comment"})
    void zeroCounterWriteCannotCommitNewFactWhileRepairingHistoricalDrift(String kind) {
        toggle(kind,7);
        jdbc.update(kind.equals("comment") ? "update cms_comment set like_count=42 where id=20" : "update cms_article set like_count=42,collect_count=42 where id=10");
        clearInvocations(redis,redis.opsForValue()); hook.zeroMethod=counterMethod(kind);
        assertThrows(BusinessException.class, () -> toggle(kind,8));
        assertEquals(1,facts(kind)); assertEquals(42,counter(kind)); verifyNoInteractions(redis);
        hook.zeroMethod=null;
        assertEquals(2L,toggle(kind,8).get(kind.equals("collect") ? "collectCount" : "likeCount"));
        assertEquals(2,facts(kind)); assertEquals(2,counter(kind));
    }
    @ParameterizedTest @ValueSource(strings={"like","collect","comment"})
    void outerRepeatableReadBeforeAnotherUserInsertReconcilesCommittedFacts(String kind) throws Exception {
        Assumptions.assumeTrue(isMySql(), "H2 MVCC in Repeatable Read reads from initial snapshot even with FOR UPDATE; verified on MySQL 8.0.45 in InteractionMySqlVendorTest");
        toggle(kind,7);
        var outer=tx(); outer.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
        var executor=Executors.newSingleThreadExecutor();
        try {
            outer.executeWithoutResult(status -> {
                assertEquals(1,facts(kind));
                assertDoesNotThrow(() -> executor.submit(() -> toggle(kind,8)).get(15,TimeUnit.SECONDS));
                clearInvocations(redis,redis.opsForValue());
                // Actual stale consistent read before the locking scalar and outer toggle.
                assertEquals(1,facts(kind),"Plain count must remain stale after the other committed insertion");
                assertAll("Do not accept a stale scalar or a stale authoritative counter",
                        () -> assertEquals(2,scalarFactCount(kind),"Scalar must see the other committed fact"),
                        () -> assertEquals(3L,toggle(kind,9).get(kind.equals("collect") ? "collectCount" : "likeCount"),"Proxied toggle must reconcile all three facts"),
                        () -> assertEquals(3,counter(kind),"Authoritative counter must equal the actual three facts"),
                        () -> verifyNoInteractions(redis));
            });
            assertEquals(3,facts(kind)); assertEquals(3,counter(kind));
        } finally { executor.shutdownNow(); assertTrue(executor.awaitTermination(15,TimeUnit.SECONDS)); }
    }
}
