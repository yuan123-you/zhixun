package com.zhixun.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhixun.common.util.SecurityUtil;
import com.zhixun.common.util.SensitiveWordUtil;
import com.zhixun.dto.article.*;
import com.zhixun.entity.Article;
import com.zhixun.enums.ArticleStatusEnum;
import com.zhixun.mapper.*;
import com.zhixun.service.FeedService;
import com.zhixun.service.OpenSearchSyncService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Answers;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ArticleCommitEffectsTest {
    private AtomicInteger writes;
    private Article article;
    private ArticleMapper articles;
    private OpenSearchSyncService search;
    private FeedService feed;
    private StringRedisTemplate redis;
    private RabbitTemplate broker;
    private ArticleServiceImpl service;

    @BeforeEach void setUp() {
        writes = new AtomicInteger(1);
        article = new Article(); article.setId(10L); article.setAuthorId(7L);
        article.setTitle("Original"); article.setContent("Content"); article.setStatus(ArticleStatusEnum.PUBLISHED);
        article.setPublishAt(LocalDateTime.now().minusMinutes(5));
        article.setReviewState("CLEARED");
        articles = mock(ArticleMapper.class, inv -> {
            if (inv.getMethod().getName().equals("selectForUpdate")) return article;
            if (inv.getMethod().getReturnType() == int.class) {
                if (inv.getMethod().getName().equals("insert")) ((Article) inv.getArgument(0)).setId(10L);
                return writes.get();
            }
            return Answers.RETURNS_DEFAULTS.answer(inv);
        });
        when(articles.selectById(10L)).thenReturn(article);
        when(articles.selectList(any())).thenReturn(List.of(article));
        SensitiveWordUtil sensitive = mock(SensitiveWordUtil.class);
        when(sensitive.filter(anyString())).thenAnswer(inv -> inv.getArgument(0));
        SecurityUtil security = mock(SecurityUtil.class);
        when(security.isAdmin()).thenReturn(true); when(security.isOwnerOrAdmin(anyLong())).thenReturn(true);
        search = mock(OpenSearchSyncService.class); feed = mock(FeedService.class);
        redis = mock(StringRedisTemplate.class); broker = mock(RabbitTemplate.class);
        @SuppressWarnings("unchecked") Cursor<String> cursor = mock(Cursor.class);
        when(redis.scan(any())).thenReturn(cursor);
        @SuppressWarnings("unchecked") ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values); when(values.increment(anyString())).thenReturn(1L);
        service = new ArticleServiceImpl(articles, mock(ArticleTagMapper.class), mock(ArticleLikeMapper.class),
                mock(ArticleViewHistoryMapper.class), mock(CollectMapper.class), mock(UserFollowMapper.class),
                mock(ArticleOperateLogMapper.class), mock(UserMapper.class), mock(CategoryMapper.class), mock(TagMapper.class),
                mock(ArticleImageMapper.class), sensitive, security, search, feed, redis, broker, new ObjectMapper());
    }

    private void transaction(Runnable work) {
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);
        try { work.run(); }
        finally { TransactionSynchronizationManager.clearSynchronization(); TransactionSynchronizationManager.setActualTransactionActive(false); }
    }
    private void command(String command) {
        switch (command) {
            case "create" -> {
                ArticleCreateRequest request = new ArticleCreateRequest(); request.setTitle("Created"); request.setContent("Content");
                request.setCategoryId(1L); request.setStatus(2);
                service.createArticle(7L, request, "127.0.0.1");
            }
            case "edit" -> {
                ArticleUpdateRequest request = new ArticleUpdateRequest(); request.setTitle("Edited"); request.setContent("Content");
                request.setCategoryId(1L); service.updateArticle(7L, 10L, request);
            }
            case "delete" -> service.deleteArticle(7L, 10L);
            case "approve", "offline" -> {
                ArticleStatusRequest request = new ArticleStatusRequest(); request.setStatus(command.equals("approve") ? 2 : 4);
                if (command.equals("offline")) request.setRejectReason("Moderation reason");
                service.updateArticleStatus(7L, 10L, request);
            }
            case "scheduled" -> { article.setStatus(ArticleStatusEnum.PENDING); service.publishScheduledArticles(); }
            case "draft" -> { article.setReviewState("CLEARED"); article.setStatus(ArticleStatusEnum.DRAFT); service.publishDraft(7L, 10L, new ArticlePublishRequest()); }
            case "visibility" -> {
                ArticleVisibilityRequest request = new ArticleVisibilityRequest(); request.setVisibility(3);
                service.updateArticleVisibility(7L, 10L, request);
            }
            default -> throw new AssertionError(command);
        }
    }

    @ParameterizedTest @ValueSource(strings = {"create", "edit", "delete", "approve", "offline", "scheduled", "draft", "visibility"})
    void externalEffectsDoNotRunBeforeTheOuterCommit(String command) {
        transaction(() -> {
            command(command);
            verifyNoInteractions(search, feed, redis, broker);
            TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
            assertTrue(mockingDetails(search).getInvocations().size() + mockingDetails(feed).getInvocations().size()
                    + mockingDetails(redis).getInvocations().size() + mockingDetails(broker).getInvocations().size() > 0);
        });
    }

    @ParameterizedTest @ValueSource(strings = {"create", "edit", "delete", "approve", "offline", "scheduled", "draft", "visibility"})
    void rolledBackArticleChangesDoNotEscapeThroughExternalEffects(String command) {
        transaction(() -> {
            command(command);
            TransactionSynchronizationManager.getSynchronizations().forEach(callback -> callback.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
            verifyNoInteractions(search, feed, redis, broker);
        });
    }

    @Test void scheduledZeroRowWriteCannotAnnouncePublication() {
        writes.set(0); article.setStatus(ArticleStatusEnum.PENDING);
        service.publishScheduledArticles();
        verifyNoInteractions(search, feed, redis, broker);
        assertEquals(ArticleStatusEnum.PENDING, article.getStatus());
    }

    @Test void scheduledApprovedPublicationRestoresImagesOnlyAfterCommit() {
        article.setReviewState("APPROVED");
        transaction(() -> {
            command("scheduled");
            verifyNoInteractions(search, feed, redis, broker);
            TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
            verify(search).syncArticle(10L);
            verify(search).syncImagesByArticle(10L);
            verify(feed).fanoutOnPublish(eq(7L), eq(10L), any());
        });
    }

    @Test void searchFailureAfterCommitDoesNotSkipIndependentImageFeedBrokerOrCacheEffects() {
        doThrow(new IllegalStateException("search unavailable")).when(search).syncArticle(10L);
        transaction(() -> {
            command("approve");
            assertDoesNotThrow(() -> TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit));
            verify(search).syncImagesByArticle(10L);
            verify(feed).fanoutOnPublish(eq(7L), eq(10L), any());
            verify(broker).convertAndSend(anyString(), anyString(), any(Object.class));
            verify(redis, atLeastOnce()).delete(anyString());
        });
    }

    @Test void articleCacheFailureDoesNotPreventFeedCacheInvalidation() {
        doThrow(new IllegalStateException("cache unavailable")).when(redis).delete("article:view:10");
        transaction(() -> {
            command("edit");
            assertDoesNotThrow(() -> TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit));
            verify(feed).clearFeedCaches();
            verify(redis.opsForValue()).increment(anyString());
        });
    }

    @Test void deferredPublishedMetadataDoesNotFollowLaterEntityMutation() {
        LocalDateTime originalTime = article.getPublishAt();
        transaction(() -> {
            command("approve");
            article.setAuthorId(99L); article.setPublishAt(originalTime.plusDays(1));
            TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
            verify(feed).fanoutOnPublish(7L, 10L, originalTime);
        });
    }
}
