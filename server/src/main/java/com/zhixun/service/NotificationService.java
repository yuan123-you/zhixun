package com.zhixun.service;

import com.zhixun.common.result.PageResult;
import com.zhixun.vo.NotificationVO;

import java.util.List;

/**
 * 通知服务接口
 */
public interface NotificationService {

    /**
     * 创建通知
     *
     * @param userId    接收用户ID
     * @param type      通知类型
     * @param title     通知标题
     * @param content   通知内容
     * @param relatedId 关联业务ID
     */
    void createNotification(Long userId, Integer type, String title, String content, Long relatedId);

    /**
     * 创建通知（带分组键）
     *
     * @param userId    接收用户ID
     * @param type      通知类型
     * @param title     通知标题
     * @param content   通知内容
     * @param relatedId 关联业务ID
     * @param groupKey  分组键
     */
    void createNotification(Long userId, Integer type, String title, String content, Long relatedId, String groupKey);

    /**
     * 创建通知（带发送者和群发标记，用于管理员发送）
     *
     * @param userId    接收用户ID
     * @param type      通知类型
     * @param title     通知标题
     * @param content   通知内容
     * @param relatedId 关联业务ID
     * @param groupKey  分组键
     * @param senderId  发送者ID
     * @param targetAll 是否群发
     */
    void createNotification(Long userId, Integer type, String title, String content, Long relatedId, String groupKey, Long senderId, boolean targetAll);

    /**
     * 获取通知列表
     *
     * @param userId   用户ID
     * @param type     通知类型（可选）
     * @param page     页码
     * @param pageSize 每页大小
     * @return 通知分页列表
     */
    PageResult<NotificationVO> getNotifications(Long userId, Integer type, Integer page, Integer pageSize);

    /**
     * 标记已读
     *
     * @param userId         用户ID
     * @param notificationId 通知ID
     */
    void markAsRead(Long userId, Long notificationId);

    /**
     * 全部标记已读
     *
     * @param userId 用户ID
     */
    void markAllAsRead(Long userId);

    /**
     * 获取未读通知数
     *
     * @param userId 用户ID
     * @return 未读数
     */
    Integer getUnreadCount(Long userId);

    /**
     * 删除通知
     *
     * @param userId         用户ID
     * @param notificationId 通知ID
     */
    void deleteNotification(Long userId, Long notificationId);

    /**
     * 批量标记已读
     *
     * @param userId          用户ID
     * @param notificationIds 通知ID列表
     */
    void batchMarkAsRead(Long userId, List<Long> notificationIds);

    /**
     * 批量删除通知
     *
     * @param userId          用户ID
     * @param notificationIds 通知ID列表
     */
    void batchDeleteNotifications(Long userId, List<Long> notificationIds);
}
