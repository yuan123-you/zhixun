package com.zhixun.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 数据概览视图
 */
@Data
public class DashboardVO {

    /** 用户总数 */
    private Long userTotal;

    /** 作品总数 */
    private Long articleTotal;

    /** 今日日活用户数 */
    private Long todayDau;

    /** 今日浏览量 */
    private Long todayView;

    /** 今日点赞数 */
    private Long todayLike;

    /** 今日评论数 */
    private Long todayComment;

    /** 近7天趋势数据 */
    private TrendData trend;

    /** 用户留存率数据（第1-7天） */
    private List<RetentionRate> retentionRates;

    /** 用户活跃度分布 */
    private List<ActivityDistribution> activityDistributions;

    /** 增长趋势数据（支持日/周/月维度） */
    private List<GrowthTrend> growthTrends;

    /** 分类作品分布 */
    private List<CategoryDistribution> categoryDistributions;

    /** 热门作品排行 */
    private List<HotArticleRank> hotArticleRanks;

    /** 创作者排行 */
    private List<CreatorRank> creatorRanks;

    /**
     * 趋势数据（按日期分列的数组格式）
     */
    @Data
    public static class TrendData {

        /** 日期列表 */
        private List<String> dates;

        /** 浏览量列表 */
        private List<Long> views;

        /** 用户数列表 */
        private List<Long> users;
    }

    /**
     * 留存率
     */
    @Data
    public static class RetentionRate {

        /** 留存天数（1-7） */
        private Integer day;

        /** 留存率（0-100） */
        private BigDecimal rate;
    }

    /**
     * 活跃度分布
     */
    @Data
    public static class ActivityDistribution {

        /** 活跃度等级：high/medium/low */
        private String level;

        /** 用户数 */
        private Long count;

        /** 占比百分比 */
        private BigDecimal percentage;
    }

    /**
     * 增长趋势（支持日/周/月时间维度）
     */
    @Data
    public static class GrowthTrend {

        /** 时间标签（日期/周/月） */
        private String periodLabel;

        /** 时间维度：daily/weekly/monthly */
        private String dimension;

        /** 新增用户数 */
        private Long newUserCount;

        /** 新增作品数 */
        private Long newArticleCount;

        /** 浏览量 */
        private Long viewCount;
    }

    /**
     * 分类分布
     */
    @Data
    public static class CategoryDistribution {

        /** 分类ID */
        private Long categoryId;

        /** 分类名称 */
        private String categoryName;

        /** 作品数量 */
        private Long articleCount;

        /** 占比百分比 */
        private BigDecimal percentage;
    }

    /**
     * 热门作品排行
     */
    @Data
    public static class HotArticleRank {

        /** 作品ID */
        private Long articleId;

        /** 作品标题 */
        private String title;

        /** 作者昵称 */
        private String authorName;

        /** 浏览量 */
        private Long viewCount;

        /** 点赞数 */
        private Long likeCount;

        /** 评论数 */
        private Long commentCount;

        /** 排名 */
        private Integer rank;
    }

    /**
     * 创作者排行
     */
    @Data
    public static class CreatorRank {

        /** 用户ID */
        private Long userId;

        /** 昵称 */
        private String nickname;

        /** 头像 */
        private String avatar;

        /** 作品总数 */
        private Long articleCount;

        /** 总浏览量 */
        private Long totalViews;

        /** 总点赞数 */
        private Long totalLikes;

        /** 粉丝数 */
        private Integer followerCount;

        /** 排名 */
        private Integer rank;
    }
}
