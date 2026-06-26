package com.zhixun.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 轮播图视图
 */
@Data
public class BannerVO {

    /** 轮播图ID */
    private Long id;

    /** 标题 */
    private String title;

    /** 图片地址 */
    private String imageUrl;

    /** 链接地址 */
    private String linkUrl;

    /** 链接类型：1-作品，2-外链 */
    private Integer linkType;

    /** 排序值 */
    private Integer sortOrder;

    /** 开始展示时间 */
    private LocalDateTime startTime;

    /** 结束展示时间 */
    private LocalDateTime endTime;

    /** 状态：0-禁用，1-启用 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
