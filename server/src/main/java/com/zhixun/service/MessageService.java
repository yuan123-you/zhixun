package com.zhixun.service;

import com.zhixun.common.result.PageResult;
import com.zhixun.dto.social.MessageSendRequest;
import com.zhixun.vo.ConversationVO;
import com.zhixun.vo.MessageVO;

/**
 * 私信服务接口
 */
public interface MessageService {

    /**
     * 发送私信
     *
     * @param senderId 发送者ID
     * @param request  消息发送请求
     * @return 消息视图
     */
    MessageVO sendMessage(Long senderId, MessageSendRequest request);

    /**
     * 发送AI助手消息（私信场景）
     *
     * @param senderId     当前用户ID
     * @param targetUserId 会话目标用户ID
     * @param question     AI提问内容
     * @return AI回复消息视图
     */
    MessageVO sendAIMessage(Long senderId, Long targetUserId, String question);

    /**
     * 获取会话列表
     *
     * @param userId   用户ID
     * @param page     页码
     * @param pageSize 每页大小
     * @return 会话分页列表
     */
    PageResult<ConversationVO> getConversations(Long userId, Integer page, Integer pageSize);

    /**
     * 获取与某用户的私信记录
     *
     * @param userId       当前用户ID
     * @param targetUserId 对方用户ID
     * @param page         页码
     * @param pageSize     每页数量
     * @return 消息列表
     */
    PageResult<MessageVO> getMessages(Long userId, Long targetUserId, Integer page, Integer pageSize);

    /**
     * 标记与某用户的私信已读
     *
     * @param userId       当前用户ID
     * @param targetUserId 对方用户ID
     */
    void markAsRead(Long userId, Long targetUserId);

    /**
     * 获取未读私信总数
     *
     * @param userId 用户ID
     * @return 未读数
     */
    Integer getUnreadCount(Long userId);
}
