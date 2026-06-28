package com.zhixun.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会话视图（客户端和管理端共用）
 * <p>
 * 客户端使用字段：userId, nickname, avatar, lastMessage, unreadCount, lastMessageTime
 * 管理端使用字段：id, user1Id, user1Name, user2Id, user2Name, lastMessage, messageCount, createdAt, updatedAt
 */
@Data
public class ConversationVO {

    // ===== 管理端字段 =====

    /** 会话ID */
    private Long id;

    /** 用户1 ID */
    private Long user1Id;

    /** 用户1名称 */
    private String user1Name;

    /** 用户2 ID */
    private Long user2Id;

    /** 用户2名称 */
    private String user2Name;

    /** 消息总数 */
    private Integer messageCount;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;

    // ===== 客户端字段 =====

    /** 对方用户信息 */
    private UserVO user;

    /** 对方用户ID */
    private Long userId;

    /** 对方昵称 */
    private String nickname;

    /** 对方头像 */
    private String avatar;

    /** 最后一条消息内容 */
    private String lastMessage;

    /** 未读消息数 */
    private Integer unreadCount;

    /** 最后消息时间 */
    private LocalDateTime lastMessageTime;
}