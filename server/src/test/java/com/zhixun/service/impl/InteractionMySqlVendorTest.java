package com.zhixun.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.zhixun.common.util.SecurityUtil;
import com.zhixun.entity.*;
import com.zhixun.mapper.*;
import java.io.*;
import java.net.Socket;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import javax.sql.DataSource;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mybatis.spring.mapper.MapperFactoryBean;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Native MySQL 8.0 / InnoDB vendor verification.
 * Verifies that on real MySQL InnoDB, locking reads under Repeatable Read
 * are CURRENT reads seeing committed facts and reconciling counters accurately.
 */
class InteractionMySqlVendorTest {

    private static Properties props;
    private static boolean mysqlAvailable = false;

    @BeforeAll
    static void checkMySql() {
        try {
            Path targetDir = Paths.get("target");
            if (!Files.exists(targetDir)) return;
            try (var stream = Files.find(targetDir, 3, (p, a) -> p.getFileName().toString().equals("mysql-test.properties"))) {
                var opt = stream.findFirst();
                if (opt.isPresent()) {
                    props = new Properties();
                    try (InputStream in = Files.newInputStream(opt.get())) {
                        props.load(in);
                    }
                    int port = Integer.parseInt(props.getProperty("port", "40895"));
                    try (Socket s = new Socket("127.0.0.1", port)) {
                        mysqlAvailable = true;
                    } catch (IOException ignored) {}
                }
            }
        } catch (Exception ignored) {}
    }

    @Configuration
    @EnableTransactionManagement(proxyTargetClass = true)
    static class MySqlConfig {
        @Bean
        DataSource source() {
            int port = Integer.parseInt(props.getProperty("port", "40895"));
            String schema = props.getProperty("schema", "test");
            String user = props.getProperty("user", "fixture_user");
            String pass = props.getProperty("password", "");
            String url = "jdbc:mysql://127.0.0.1:" + port + "/" + schema + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
            var ds = new DriverManagerDataSource(url, user, pass);
            ds.setDriverClassName("com.mysql.cj.jdbc.Driver");
            return ds;
        }

        @Bean JdbcTemplate jdbc(DataSource source) { return new JdbcTemplate(source); }
        @Bean PlatformTransactionManager transactionManager(DataSource source) { return new DataSourceTransactionManager(source); }

        @Bean SqlSessionFactory sqlSessionFactory(DataSource source) throws Exception {
            var bean = new MybatisSqlSessionFactoryBean();
            bean.setDataSource(source);
            var config = new MybatisConfiguration();
            config.setMapUnderscoreToCamelCase(true);
            bean.setConfiguration(config);
            return bean.getObject();
        }

        private <T> T mapper(Class<T> type, SqlSessionFactory factory) throws Exception {
            var bean = new MapperFactoryBean<T>(type);
            bean.setSqlSessionFactory(factory);
            bean.afterPropertiesSet();
            return bean.getObject();
        }

        @Bean ArticleMapper articles(SqlSessionFactory factory) throws Exception { return mapper(ArticleMapper.class, factory); }
        @Bean ArticleLikeMapper likes(SqlSessionFactory factory) throws Exception { return mapper(ArticleLikeMapper.class, factory); }
        @Bean CollectMapper collects(SqlSessionFactory factory) throws Exception { return mapper(CollectMapper.class, factory); }
        @Bean CommentMapper comments(SqlSessionFactory factory) throws Exception { return mapper(CommentMapper.class, factory); }

        @Bean SecurityUtil security() { return mock(SecurityUtil.class); }

        @Bean StringRedisTemplate redis() {
            var redis = mock(StringRedisTemplate.class);
            @SuppressWarnings("unchecked") ValueOperations<String,String> values = mock(ValueOperations.class);
            when(redis.opsForValue()).thenReturn(values);
            when(redis.scan(any())).thenAnswer(invocation -> mock(Cursor.class));
            return redis;
        }

        @Bean LikeServiceImpl likeService(ArticleLikeMapper likes, ArticleMapper articles, CommentMapper comments, StringRedisTemplate redis, SecurityUtil security) {
            return new LikeServiceImpl(likes, articles, security, comments, mock(ArticleTagMapper.class), mock(TagMapper.class), mock(UserMapper.class), mock(CategoryMapper.class), redis);
        }

        @Bean CollectServiceImpl collectService(CollectMapper collects, ArticleMapper articles, StringRedisTemplate redis, SecurityUtil security) {
            return new CollectServiceImpl(collects, articles, security, mock(ArticleTagMapper.class), mock(TagMapper.class), mock(UserMapper.class), mock(CategoryMapper.class));
        }
    }

    private AnnotationConfigApplicationContext context;
    private JdbcTemplate jdbc;
    private LikeServiceImpl likes;
    private CollectServiceImpl collects;
    private ArticleLikeMapper likeMapper;
    private CollectMapper collectMapper;

    @BeforeEach
    void setup() {
        Assumptions.assumeTrue(mysqlAvailable, "Native MySQL fixture not running; skipping vendor tests");
        context = new AnnotationConfigApplicationContext(MySqlConfig.class);
        jdbc = context.getBean(JdbcTemplate.class);
        likes = context.getBean(LikeServiceImpl.class);
        collects = context.getBean(CollectServiceImpl.class);
        likeMapper = context.getBean(ArticleLikeMapper.class);
        collectMapper = context.getBean(CollectMapper.class);

        // Drop and recreate fresh tables for each test
        jdbc.execute("DROP TABLE IF EXISTS cms_like");
        jdbc.execute("DROP TABLE IF EXISTS cms_collect");
        jdbc.execute("DROP TABLE IF EXISTS cms_comment");
        jdbc.execute("DROP TABLE IF EXISTS cms_article");

        jdbc.execute("CREATE TABLE cms_article (id BIGINT PRIMARY KEY, author_id BIGINT DEFAULT 1, title VARCHAR(100) DEFAULT 'test', content TEXT, status INT DEFAULT 2, review_state VARCHAR(20) DEFAULT 'CLEARED', visibility INT DEFAULT 1, like_count BIGINT DEFAULT 0, collect_count BIGINT DEFAULT 0, deleted_at DATETIME NULL) ENGINE=InnoDB");
        jdbc.execute("CREATE TABLE cms_comment (id BIGINT PRIMARY KEY, article_id BIGINT DEFAULT 10, user_id BIGINT DEFAULT 1, parent_id BIGINT DEFAULT 0, reply_to_id BIGINT DEFAULT 0, content TEXT, images JSON, status INT DEFAULT 1, like_count BIGINT DEFAULT 0, created_at DATETIME DEFAULT CURRENT_TIMESTAMP, deleted_at DATETIME NULL) ENGINE=InnoDB");
        jdbc.execute("CREATE TABLE cms_like (id BIGINT AUTO_INCREMENT PRIMARY KEY, user_id BIGINT, target_id BIGINT, target_type INT, created_at DATETIME DEFAULT CURRENT_TIMESTAMP, UNIQUE KEY uk_like (user_id, target_id, target_type), INDEX idx_target (target_id, target_type)) ENGINE=InnoDB");
        jdbc.execute("CREATE TABLE cms_collect (id BIGINT AUTO_INCREMENT PRIMARY KEY, user_id BIGINT, article_id BIGINT, group_name VARCHAR(50) DEFAULT 'default', created_at DATETIME DEFAULT CURRENT_TIMESTAMP, UNIQUE KEY uk_collect (user_id, article_id), INDEX idx_article (article_id)) ENGINE=InnoDB");

        // Seed initial targets
        jdbc.execute("INSERT INTO cms_article (id, author_id, title, content, status, review_state, visibility, like_count, collect_count) VALUES (10, 1, 'Article 10', 'Article content', 2, 'CLEARED', 0, 0, 0)");
        jdbc.execute("INSERT INTO cms_comment (id, article_id, user_id, content, like_count) VALUES (20, 10, 1, 'Comment 20', 0)");
    }

    @AfterEach
    void teardown() {
        if (context != null) context.close();
    }

    private TransactionTemplate tx() {
        return new TransactionTemplate(context.getBean(PlatformTransactionManager.class));
    }

    private Map<String, Object> toggle(String kind, long userId) {
        return kind.equals("collect") ? collects.toggleCollect(userId, 10L, null)
                : likes.toggleLike(userId, kind.equals("comment") ? 20L : 10L, kind.equals("comment") ? 2 : 1);
    }

    private int facts(String kind) {
        return kind.equals("collect")
                ? jdbc.queryForObject("SELECT COUNT(*) FROM cms_collect WHERE article_id=10", Integer.class)
                : jdbc.queryForObject("SELECT COUNT(*) FROM cms_like WHERE target_id=" + (kind.equals("comment") ? 20 : 10) + " AND target_type=" + (kind.equals("comment") ? 2 : 1), Integer.class);
    }

    private int counter(String kind) {
        return kind.equals("comment")
                ? jdbc.queryForObject("SELECT like_count FROM cms_comment WHERE id=20", Integer.class)
                : jdbc.queryForObject(kind.equals("collect") ? "SELECT collect_count FROM cms_article WHERE id=10" : "SELECT like_count FROM cms_article WHERE id=10", Integer.class);
    }

    private long scalarFactCount(String kind) {
        return kind.equals("collect") ? collectMapper.countTargetFactsForUpdate(10L)
                : likeMapper.countTargetFactsForUpdate(kind.equals("comment") ? 20L : 10L, kind.equals("comment") ? 2 : 1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"like", "collect", "comment"})
    void mysqlRepeatableReadCurrentReadReconcilesConcurrentCommittedFacts(String kind) throws Exception {
        // Step 1: User 7 toggles on
        toggle(kind, 7);
        assertEquals(1, facts(kind));
        assertEquals(1, counter(kind));

        // Step 2: Txn A starts with Repeatable Read and establishes an MVCC snapshot
        var outer = tx();
        outer.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
        var executor = Executors.newSingleThreadExecutor();
        try {
            outer.executeWithoutResult(status -> {
                // Initial consistent read in Txn A observes 1 fact
                assertEquals(1, facts(kind));

                // Step 3: Concurrently on another connection/transaction, user 8 toggles and commits
                assertDoesNotThrow(() -> executor.submit(() -> {
                    tx().executeWithoutResult(other -> {
                        assertTrue((Boolean) toggle(kind, 8).get(kind.equals("collect") ? "isCollected" : "isLiked"));
                        assertEquals(2, facts(kind));
                        assertEquals(2, counter(kind));
                    });
                }).get(15, TimeUnit.SECONDS));

                // Step 4: In Txn A, plain consistent read STILL sees 1 (MVCC snapshot)
                assertEquals(1, facts(kind), "Plain non-locking read remains snapshot-consistent");

                // Step 5: In Txn A, the locking scalar read is a CURRENT READ on InnoDB and sees 2!
                assertEquals(2, scalarFactCount(kind), "InnoDB locking read is a current read seeing committed facts");

                // Step 6: User 9 toggles in Txn A. It locks the target and reconciles all 3 facts!
                String countKey = kind.equals("collect") ? "collectCount" : "likeCount";
                assertEquals(3L, toggle(kind, 9).get(countKey));
                assertEquals(3, counter(kind));
            });

            // After Txn A commits: final state has 3 facts and counter is 3
            assertEquals(3, facts(kind));
            assertEquals(3, counter(kind));
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(15, TimeUnit.SECONDS));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"like", "collect", "comment"})
    void mysqlDirectCountForUpdateReturnsCurrentCommittedCount(String kind) {
        toggle(kind, 7);
        toggle(kind, 8);
        assertEquals(2, facts(kind));

        tx().executeWithoutResult(status -> {
            long count = scalarFactCount(kind);
            assertEquals(2, count);
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"like", "collect", "comment"})
    void mysqlRollbackRestoresPreviousFactAndCounter(String kind) {
        toggle(kind, 7);
        assertEquals(1, facts(kind));
        assertEquals(1, counter(kind));

        tx().executeWithoutResult(status -> {
            toggle(kind, 8);
            assertEquals(2, facts(kind));
            assertEquals(2, counter(kind));
            status.setRollbackOnly();
        });

        // After rollback, user 8's toggle is gone and counter restored
        assertEquals(1, facts(kind));
        assertEquals(1, counter(kind));
    }
}
