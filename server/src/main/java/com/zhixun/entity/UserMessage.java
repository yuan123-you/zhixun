package com.zhixun.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 私信实体，对应 user_message 表
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("user_message")
public class UserMessage {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 发送者ID，对应 sender_id */
    private Long senderId;

    /** 接收者ID，对应 receiver_id */
    private Long receiverId;

    /** 会话ID */
    @TableField(exist = false)
    private Long conversationId;

    /** 消息类型 */
    @TableField(exist = false)
    private String type;

    /** 消息内容 */
    private String content;

    /** 是否已读：0-未读，1-已读，对应 is_read */
    private Integer isRead;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
