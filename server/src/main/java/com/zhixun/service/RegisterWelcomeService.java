package com.zhixun.service;

/**
 * 新用户注册欢迎服务接口
 * <p>
 * 负责新用户注册后的自动关注官方账号、发送欢迎通知和欢迎私信。
 * 所有操作在同一事务中执行，确保关注状态与消息推送的事务一致性。
 * </p>
 */
public interface RegisterWelcomeService {

    /**
     * 处理新用户注册后的欢迎流程
     * <ul>
     *   <li>自动关注知讯官方账号</li>
     *   <li>发送系统欢迎通知</li>
     *   <li>发送官方欢迎私信</li>
     * </ul>
     *
     * @param userId 新注册用户ID
     */
    void handleNewUserRegistration(Long userId);
}
