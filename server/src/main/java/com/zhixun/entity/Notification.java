package com.zhixun.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.zhixun.enums.NotificationTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 通知消息实体，对应 sys_notification 表
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_notification")
public class Notification {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 接收用户ID，对应 user_id */
    private Long userId;

    /** 通知类型 */
    private NotificationTypeEnum type;

    /** 通知标题 */
    private String title;

    /** 通知内容 */
    private String content;

    /** 是否已读：0-未读，1-已读，对应 is_read */
    private Integer isRead;

    /** 关联业务ID，对应 related_id */
    private Long relatedId;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
