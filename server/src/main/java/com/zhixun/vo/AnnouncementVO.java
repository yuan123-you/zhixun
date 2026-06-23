package com.zhixun.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 公告视图
 */
@Data
public class AnnouncementVO {

    /** 公告ID */
    private Long id;

    /** 标题 */
    private String title;

    /** 内容 */
    private String content;

    /** 类型：1-系统公告，2-活动公告 */
    private Integer type;

    /** 是否置顶 */
    private Integer isTop;

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
