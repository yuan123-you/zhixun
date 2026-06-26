package com.zhixun.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 热榜作品视图
 */
@Data
public class HotArticleVO {

    /** 作品ID */
    private Long id;

    /** 标题 */
    private String title;

    /** 浏览量 */
    private Integer viewCount;

    /** 点赞数 */
    private Integer likeCount;

    /** 评论数 */
    private Integer commentCount;

    /** 热度分数 */
    private Double score;

    /** 作者昵称 */
    private String authorNickname;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
