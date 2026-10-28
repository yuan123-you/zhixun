package com.zhixun.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhixun.entity.Article;
import com.zhixun.enums.ArticleVisibilityEnum;

/** Applies the same viewer permission before SQL counting, ordering and pagination. */
public final class ArticleVisibilityQuery {
    private ArticleVisibilityQuery() {}

    public static void apply(LambdaQueryWrapper<Article> query, Long viewerId) {
        query.and(scope -> {
            scope.isNull(Article::getVisibility).or().eq(Article::getVisibility, ArticleVisibilityEnum.PUBLIC.getValue());
            if (viewerId == null) return;
            scope.or().eq(Article::getAuthorId, viewerId);
            String followsAuthor = "SELECT 1 FROM user_follow vf WHERE vf.follower_id = {0} AND vf.following_id = cms_article.author_id";
            String authorFollows = "SELECT 1 FROM user_follow af WHERE af.follower_id = cms_article.author_id AND af.following_id = {0}";
            scope.or(follower -> follower.eq(Article::getVisibility, ArticleVisibilityEnum.FOLLOWERS.getValue())
                    .exists(followsAuthor, viewerId));
            scope.or(mutual -> mutual.eq(Article::getVisibility, ArticleVisibilityEnum.MUTUAL.getValue())
                    .exists(followsAuthor, viewerId).exists(authorFollows, viewerId));
        });
    }
}
