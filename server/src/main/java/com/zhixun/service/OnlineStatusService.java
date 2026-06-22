package com.zhixun.service;

/**
 * 在线状态服务接口
 */
public interface OnlineStatusService {

    /**
     * 更新在线状态
     *
     * @param userId 用户ID
     * @param online 是否在线
     */
    void updateOnlineStatus(Long userId, boolean online);

    /**
     * 获取在线状态（考虑隐私设置）
     *
     * @param userId 用户ID
     * @return true-在线
     */
    boolean getOnlineStatus(Long userId);

    /**
     * 心跳（更新 last_active_at）
     *
     * @param userId 用户ID
     */
    void heartbeat(Long userId);
}
