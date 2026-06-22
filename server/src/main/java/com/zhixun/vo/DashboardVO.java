package com.zhixun.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * 数据概览视图
 */
@Data
public class DashboardVO {

    /** 用户总数 */
    @JsonProperty("user_total")
    private Long userTotal;

    /** 文章总数 */
    @JsonProperty("article_total")
    private Long articleTotal;

    /** 今日日活用户数 */
    @JsonProperty("today_dau")
    private Long todayDau;

    /** 今日浏览量 */
    @JsonProperty("today_view")
    private Long todayView;

    /** 今日点赞数 */
    @JsonProperty("today_like")
    private Long todayLike;

    /** 今日评论数 */
    @JsonProperty("today_comment")
    private Long todayComment;

    /** 近7天趋势数据 */
    @JsonProperty("trend")
    private TrendData trend;

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
}
