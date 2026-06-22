package com.zhixun.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会话视图
 */
@Data
public class ConversationVO {

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
