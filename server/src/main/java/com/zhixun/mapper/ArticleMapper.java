package com.zhixun.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhixun.entity.Article;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Select;
import java.time.LocalDateTime;

/**
 * 作品 Mapper
 */
@Mapper
public interface ArticleMapper extends BaseMapper<Article> {

    @Select("SELECT * FROM cms_article WHERE id=#{articleId} AND deleted_at IS NULL FOR UPDATE")
    Article selectForUpdate(@Param("articleId") Long articleId);

    @Update("UPDATE cms_article SET status=#{article.status}, review_state=#{article.reviewState}, " +
            "publish_at=#{article.publishAt}, reject_reason=#{article.rejectReason}, updated_at=#{now} " +
            "WHERE id=#{article.id} AND deleted_at IS NULL")
    int saveReviewDecision(@Param("article") Article article, @Param("now") LocalDateTime now);

    @Update("UPDATE cms_article SET status=2, updated_at=#{now} " +
            "WHERE id=#{articleId} AND status=1 AND deleted_at IS NULL " +
            "AND review_state IN ('CLEARED','APPROVED') " +
            "AND publish_at IS NOT NULL AND publish_at <= #{now}")
    int publishScheduledIfEligible(@Param("articleId") Long articleId, @Param("now") LocalDateTime now);
    /** Interaction counters are replaced only while the authoritative article row is locked. */
    @Update("UPDATE cms_article SET like_count=#{count} WHERE id=#{articleId} AND deleted_at IS NULL")
    int saveLikeCount(@Param("articleId") Long articleId, @Param("count") long count);

    @Update("UPDATE cms_article SET collect_count=#{count} WHERE id=#{articleId} AND deleted_at IS NULL")
    int saveCollectCount(@Param("articleId") Long articleId, @Param("count") long count);

    @Select("SELECT COUNT(*) FROM user_follow WHERE follower_id=#{followerId} AND following_id=#{followingId}")
    long countInteractionFollow(@Param("followerId") Long followerId, @Param("followingId") Long followingId);
}
