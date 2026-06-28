package com.zhixun.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 私信视图
 */
@Data
public class MessageVO {

    /** 消息ID */
    private Long id;

    /** 会话ID */
    private Long conversationId;

    /** 消息类型 */
    private String type;

    /** 发送者信息 */
    private UserVO sender;

    /** 发送者ID */
    private Long senderId;

    /** 发送者昵称 */
    private String senderNickname;

    /** 发送者头像 */
    private String senderAvatar;

    /** 接收者ID */
    private Long receiverId;

    /** 接收者昵称 */
    private String receiverNickname;

    /** 接收者头像 */
    private String receiverAvatar;

    /** 消息内容 */
    private String content;

    /** 是否已读 */
    private Integer isRead;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
