package com.zhixun.service;

import com.zhixun.entity.Article;
import com.zhixun.enums.ArticleStatusEnum;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Set;

/** Content permission and publication time are separate conditions; dates never grant approval. */
public final class ArticleReviewPolicy {
    public static final String UNVERIFIED = "UNVERIFIED";
    public static final String REQUIRED = "REQUIRED";
    public static final String CLEARED = "CLEARED";
    public static final String APPROVED = "APPROVED";
    private ArticleReviewPolicy() {}

    public static String state(Article article) {
        String state = article.getReviewState();
        state = state == null ? UNVERIFIED : state.trim().toUpperCase(Locale.ROOT);
        return Set.of(UNVERIFIED, REQUIRED, CLEARED, APPROVED).contains(state) ? state : UNVERIFIED;
    }
    public static boolean permitted(Article article) {
        return Set.of(CLEARED, APPROVED).contains(state(article));
    }
    private static boolean future(Article article, LocalDateTime now) {
        return article.getPublishAt() != null && article.getPublishAt().isAfter(now);
    }
    private static void selectPublicationStatus(Article article, LocalDateTime now) {
        if (!permitted(article) || future(article, now)) article.setStatus(ArticleStatusEnum.PENDING);
        else {
            article.setStatus(ArticleStatusEnum.PUBLISHED);
            if (article.getPublishAt() == null) article.setPublishAt(now);
        }
    }
    public static void created(Article article, boolean publishIntent, boolean sensitive, LocalDateTime now) {
        article.setReviewState(sensitive ? REQUIRED : CLEARED);
        if (!publishIntent && !sensitive) article.setStatus(ArticleStatusEnum.DRAFT);
        else selectPublicationStatus(article, now);
    }
    public static void submitDraft(Article article, boolean sensitive, LocalDateTime requestedDate, LocalDateTime now) {
        boolean hold = sensitive || Set.of(REQUIRED, UNVERIFIED).contains(state(article));
        article.setReviewState(hold ? REQUIRED : CLEARED);
        article.setPublishAt(requestedDate == null ? now : requestedDate);
        selectPublicationStatus(article, now);
    }
    public static void edited(Article article, boolean changed, boolean sensitive) {
        String previous = state(article);
        ArticleStatusEnum previousStatus = article.getStatus();
        boolean retainedHold = Set.of(REQUIRED, UNVERIFIED).contains(previous);
        boolean newHold = sensitive && (changed || !APPROVED.equals(previous));
        if (retainedHold || newHold || (changed && APPROVED.equals(previous))) {
            article.setReviewState(REQUIRED);
            if (previousStatus != ArticleStatusEnum.REJECTED && previousStatus != ArticleStatusEnum.OFFLINE
                    && (previousStatus != ArticleStatusEnum.DRAFT || newHold)) {
                article.setStatus(ArticleStatusEnum.PENDING);
            }
        } else if (changed || UNVERIFIED.equals(previous)) {
            article.setReviewState(CLEARED);
        }
    }
    public static void approved(Article article, LocalDateTime now) {
        article.setReviewState(APPROVED);
        article.setRejectReason(null);
        selectPublicationStatus(article, now);
    }
    public static void held(Article article, ArticleStatusEnum status, String reason) {
        article.setReviewState(REQUIRED); article.setStatus(status); article.setRejectReason(reason);
    }
}
