package com.zhixun.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhixun.common.exception.BusinessException;
import com.zhixun.common.util.SecurityUtil;
import com.zhixun.common.util.SensitiveWordUtil;
import com.zhixun.dto.article.*;
import com.zhixun.entity.Article;
import com.zhixun.enums.ArticleStatusEnum;
import com.zhixun.mapper.*;
import com.zhixun.service.FeedService;
import com.zhixun.service.OpenSearchSyncService;
import java.beans.Introspector;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Answers;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ArticleReviewSchedulingTest {
    private Article row;
    private ArticleMapper articles;
    private SensitiveWordUtil sensitive;
    private OpenSearchSyncService search;
    private FeedService feed;
    private RabbitTemplate broker;
    private ArticleServiceImpl service;
    private Article inserted;
    private int writes;
    private boolean sensitiveMatch, scanFailure, filterFailure, lostWrite;
    private ObjectMapper json;

    @BeforeEach void setUp() {
        json = new ObjectMapper().findAndRegisterModules();
        row = new Article(); row.setId(10L); row.setAuthorId(7L); row.setTitle("Original");
        row.setContent("Content"); row.setStatus(ArticleStatusEnum.DRAFT);
        articles = mock(ArticleMapper.class, inv -> {
            String method = inv.getMethod().getName();
            if (method.equals("selectForUpdate")) return row;
            if (inv.getMethod().getReturnType() == int.class) {
                writes++;
                if (method.equals("insert")) { inserted = inv.getArgument(0); inserted.setId(10L); }
                return lostWrite ? 0 : 1;
            }
            return Answers.RETURNS_DEFAULTS.answer(inv);
        });
        when(articles.selectById(10L)).thenAnswer(inv -> row);
        when(articles.selectList(any())).thenAnswer(inv -> List.of(row));
        sensitive = mock(SensitiveWordUtil.class);
        when(sensitive.filter(anyString())).thenAnswer(inv -> {
            if (filterFailure) throw new IllegalStateException("filter unavailable"); return inv.getArgument(0);
        });
        when(sensitive.containsSensitiveWord(any())).thenAnswer(inv -> {
            if (scanFailure) throw new IllegalStateException("scan unavailable"); return sensitiveMatch;
        });
        SecurityUtil security = mock(SecurityUtil.class);
        when(security.isAdmin()).thenReturn(true); when(security.isOwnerOrAdmin(anyLong())).thenReturn(true);
        search = mock(OpenSearchSyncService.class); feed = mock(FeedService.class); broker = mock(RabbitTemplate.class);
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked") Cursor<String> cursor = mock(Cursor.class);
        when(redis.scan(any())).thenReturn(cursor);
        @SuppressWarnings("unchecked") ValueOperations<String,String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values); when(values.increment(anyString())).thenReturn(1L);
        service = new ArticleServiceImpl(articles, mock(ArticleTagMapper.class), mock(ArticleLikeMapper.class),
                mock(ArticleViewHistoryMapper.class), mock(CollectMapper.class), mock(UserFollowMapper.class),
                mock(ArticleOperateLogMapper.class), mock(UserMapper.class), mock(CategoryMapper.class), mock(TagMapper.class),
                mock(ArticleImageMapper.class), sensitive, security, search, feed, redis, broker, json);
    }
    static void setReviewStateIfPresent(Article article, String value) throws Exception {
        for (var property : Introspector.getBeanInfo(Article.class).getPropertyDescriptors()) {
            if (property.getName().equals("reviewState")) property.getWriteMethod().invoke(article, value);
        }
    }
    private void noPublicationEffects() {
        verify(search, never()).syncArticle(anyLong());
        verify(search, never()).syncImagesByArticle(anyLong());
        verify(feed, never()).fanoutOnPublish(anyLong(), anyLong(), any());
    }
    private String review(Article article) { return json.valueToTree(article).path("reviewState").asText(""); }
    private ArticleCreateRequest create(LocalDateTime date) {
        var request = new ArticleCreateRequest(); request.setTitle("Title"); request.setContent("Content");
        request.setStatus(2); request.setCategoryId(1L); request.setPublishAt(date); return request;
    }
    private ArticleStatusRequest approve() { var request = new ArticleStatusRequest(); request.setStatus(2); return request; }

    @Test void futureCleanCreationWaitsUntilItsDate() {
        service.createArticle(7L, create(LocalDateTime.now().plusDays(1)), "127.0.0.1");
        assertEquals(ArticleStatusEnum.PENDING, inserted.getStatus()); assertEquals("CLEARED", review(inserted));
        noPublicationEffects();
    }
    @Test void immediateCleanCreationRetainsAutomaticPublication() {
        service.createArticle(7L, create(null), "127.0.0.1");
        assertEquals(ArticleStatusEnum.PUBLISHED, inserted.getStatus()); assertEquals("CLEARED", review(inserted));
    }
    @Test void sensitiveCreationRequiresReviewEvenWhenItsDateIsDue() {
        sensitiveMatch = true;
        service.createArticle(7L, create(LocalDateTime.now().minusHours(1)), "127.0.0.1");
        assertEquals(ArticleStatusEnum.PENDING, inserted.getStatus()); assertEquals("REQUIRED", review(inserted));
        noPublicationEffects();
    }
    @ParameterizedTest @ValueSource(strings={"REQUIRED", "UNVERIFIED"})
    void dateAloneNeverClearsAReviewHold(String state) throws Exception {
        row.setStatus(ArticleStatusEnum.PENDING); row.setPublishAt(LocalDateTime.now().minusHours(1));
        setReviewStateIfPresent(row, state);
        service.publishScheduledArticles();
        assertEquals(ArticleStatusEnum.PENDING, row.getStatus()); assertEquals(0, writes);
        noPublicationEffects();
    }
    @Test void futureApprovalIsNotEarlyPublication() throws Exception {
        row.setStatus(ArticleStatusEnum.PENDING); row.setPublishAt(LocalDateTime.now().plusDays(2));
        setReviewStateIfPresent(row, "REQUIRED");
        LocalDateTime date = row.getPublishAt();
        service.updateArticleStatus(99L, 10L, approve());
        assertEquals(ArticleStatusEnum.PENDING, row.getStatus()); assertEquals("APPROVED", review(row));
        assertEquals(date, row.getPublishAt()); noPublicationEffects();
    }
    @Test void overdueApprovalCanPublishImmediately() throws Exception {
        row.setStatus(ArticleStatusEnum.PENDING); row.setPublishAt(LocalDateTime.now().minusDays(1));
        setReviewStateIfPresent(row, "REQUIRED"); service.updateArticleStatus(99L, 10L, approve());
        assertEquals(ArticleStatusEnum.PUBLISHED, row.getStatus()); assertEquals("APPROVED", review(row));
    }
    @Test void sensitiveDraftCannotSkipReviewThroughSchedule() {
        sensitiveMatch = true;
        var request = new ArticlePublishRequest(); request.setPublishAt(LocalDateTime.now().minusMinutes(1));
        service.publishDraft(7L, 10L, request);
        assertEquals(ArticleStatusEnum.PENDING, row.getStatus()); assertEquals("REQUIRED", review(row));
        service.publishScheduledArticles(); assertEquals(ArticleStatusEnum.PENDING, row.getStatus());
        noPublicationEffects();
    }
    @Test void cleanDraftMayWaitForItsDateWithoutPretendingHumanApproval() {
        row.setReviewState("CLEARED");
        var request = new ArticlePublishRequest(); request.setPublishAt(LocalDateTime.now().plusDays(1));
        service.publishDraft(7L, 10L, request);
        assertEquals(ArticleStatusEnum.PENDING, row.getStatus()); assertEquals("CLEARED", review(row));
    }
    @Test void modifyingApprovedContentInvalidatesItsApproval() throws Exception {
        row.setStatus(ArticleStatusEnum.PUBLISHED); setReviewStateIfPresent(row, "APPROVED");
        var request = new ArticleUpdateRequest(); request.setTitle("Changed"); request.setContent("Changed content");
        service.updateArticle(7L, 10L, request);
        assertEquals(ArticleStatusEnum.PENDING, row.getStatus()); assertEquals("REQUIRED", review(row));
    }
    @Test void editingAwayASensitiveWordDoesNotSilentlyApproveARequiredHold() throws Exception {
        row.setStatus(ArticleStatusEnum.PENDING); setReviewStateIfPresent(row, "REQUIRED");
        var request = new ArticleUpdateRequest(); request.setTitle("Clean now"); request.setContent("Clean content");
        service.updateArticle(7L, 10L, request);
        assertEquals("REQUIRED", review(row));
    }
    @Test void metadataOnlyEditDoesNotWipeContentOrApprovedPermission() throws Exception {
        row.setStatus(ArticleStatusEnum.PUBLISHED); setReviewStateIfPresent(row, "APPROVED");
        var request = new ArticleUpdateRequest(); request.setLocation("Shanghai");
        service.updateArticle(7L, 10L, request);
        assertEquals("Original", row.getTitle()); assertEquals("Content", row.getContent());
        assertEquals("APPROVED", review(row)); assertEquals(ArticleStatusEnum.PUBLISHED, row.getStatus());
    }
    @Test void classifierFailureCannotBecomeCleanPublication() {
        scanFailure = true;
        assertThrows(BusinessException.class, () -> service.createArticle(7L, create(null), "127.0.0.1"));
        assertNull(inserted); noPublicationEffects();
    }
    @Test void filteringFailureCannotSaveUnfilteredContentAsClean() {
        filterFailure = true;
        assertThrows(BusinessException.class, () -> service.createArticle(7L, create(null), "127.0.0.1"));
        assertNull(inserted);
    }
    @Test void unverifiedLegacyDraftCannotAcquirePermissionByScheduling() throws Exception {
        setReviewStateIfPresent(row, "UNVERIFIED");
        var request = new ArticlePublishRequest(); request.setPublishAt(LocalDateTime.now().minusMinutes(1));
        service.publishDraft(7L, 10L, request);
        assertEquals(ArticleStatusEnum.PENDING, row.getStatus()); assertEquals("REQUIRED", review(row));
    }
    @Test void unverifiedLegacyDraftCannotBeClearedByMetadataOnlyEdit() throws Exception {
        setReviewStateIfPresent(row, "UNVERIFIED");
        var request = new ArticleUpdateRequest(); request.setLocation("Shanghai");
        service.updateArticle(7L, 10L, request);
        assertEquals("REQUIRED", review(row)); assertEquals(ArticleStatusEnum.DRAFT, row.getStatus());
    }
    @Test void revokingApprovalAlsoRemovesTheArticlesImageIndex() throws Exception {
        row.setStatus(ArticleStatusEnum.PUBLISHED); setReviewStateIfPresent(row, "APPROVED");
        var request = new ArticleUpdateRequest(); request.setTitle("Changed"); request.setContent("Changed content");
        service.updateArticle(7L, 10L, request);
        assertEquals(ArticleStatusEnum.PENDING, row.getStatus());
        verify(search).deleteArticle(10L); verify(search).deleteImagesByArticle(10L);
    }

    @Test void failedReviewWriteCannotPublishOrNotifySuccess() throws Exception {
        row.setStatus(ArticleStatusEnum.PENDING); setReviewStateIfPresent(row, "REQUIRED"); lostWrite = true;
        assertThrows(BusinessException.class, () -> service.updateArticleStatus(99L, 10L, approve()));
        verifyNoInteractions(search, feed, broker);
    }
}
