package com.zhixun.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 通知视图
 */
@Data
public class NotificationVO {

    /** 通知ID */
    private Long id;

    /** 通知类型 */
    private Integer type;

    /** 通知标题 */
    private String title;

    /** 通知内容 */
    private String content;

    /** 是否已读 */
    private Integer isRead;

    /** 关联业务ID */
    private Long relatedId;

    /** 分组键 */
    private String groupKey;

    /** 分组内通知数量 */
    private Integer groupedCount;

    /** 发送者名称 */
    private String senderName;

    /** 是否群发通知 */
    private Boolean targetAll;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
