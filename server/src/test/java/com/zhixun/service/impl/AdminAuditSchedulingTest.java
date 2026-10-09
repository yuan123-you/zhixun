package com.zhixun.service.impl;

import com.zhixun.dto.admin.AuditRequest;
import com.zhixun.entity.Article;
import com.zhixun.enums.ArticleStatusEnum;
import com.zhixun.mapper.ArticleMapper;
import com.zhixun.service.NotificationService;
import com.zhixun.service.OperationLogService;
import com.zhixun.service.ArticleService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import org.junit.jupiter.api.*;
import org.mockito.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminAuditSchedulingTest {
    @Mock ArticleMapper articles;
    @Mock NotificationService notifications;
    @Mock OperationLogService logs;
    @Mock ArticleService publication;
    @InjectMocks AdminServiceImpl service;

    @Test void immediateAlternateApprovalUsesTheSamePublicationIntegration() throws Exception {
        Article article = new Article(); article.setId(10L); article.setAuthorId(7L);
        article.setStatus(ArticleStatusEnum.PENDING); article.setPublishAt(LocalDateTime.now().minusHours(1));
        ArticleReviewSchedulingTest.setReviewStateIfPresent(article, "REQUIRED");
        when(articles.selectForUpdate(10L)).thenReturn(article); when(articles.saveReviewDecision(any(), any())).thenReturn(1);
        AuditRequest request = new AuditRequest(); request.setStatus(2);
        service.auditArticle(99L, 10L, request);
        assertEquals(ArticleStatusEnum.PUBLISHED, article.getStatus());
        assertTrue(mockingDetails(publication).getInvocations().stream()
                .anyMatch(call -> call.getMethod().getName().equals("refreshPublicationAfterCommit")
                        && call.getArgument(0).equals(10L)));
    }

    @Test void alternateAdminAuditDoesNotBypassAFutureSchedule() throws Exception {
        Article article = new Article(); article.setId(10L); article.setAuthorId(7L);
        article.setStatus(ArticleStatusEnum.PENDING); article.setPublishAt(LocalDateTime.now().plusDays(2));
        ArticleReviewSchedulingTest.setReviewStateIfPresent(article, "REQUIRED");
        when(articles.selectForUpdate(10L)).thenReturn(article);
        when(articles.saveReviewDecision(any(), any())).thenReturn(1);
        AuditRequest request = new AuditRequest(); request.setStatus(2);
        service.auditArticle(99L, 10L, request);
        assertEquals(ArticleStatusEnum.PENDING, article.getStatus());
        assertEquals("APPROVED", new ObjectMapper().findAndRegisterModules().valueToTree(article).path("reviewState").asText());
        verify(notifications).createNotification(eq(7L), anyInt(), anyString(), argThat(text -> !text.contains("现已发布")), eq(10L));
    }
}
