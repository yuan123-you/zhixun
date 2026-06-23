package com.zhixun.service;

import java.util.List;
import java.util.Map;

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
     * @param userId     目标用户ID
     * @param requesterId 请求者用户ID（自己查看自己不受隐私设置限制，可为null表示未登录）
     * @return true-在线, false-离线, null-不可见（目标用户关闭了在线状态显示）
     */
    Boolean getOnlineStatus(Long userId, Long requesterId);

    /**
     * 批量获取在线状态（考虑隐私设置）
     * 用于关注/粉丝列表等场景，一次查询多个用户的在线状态
     *
     * @param userIds    目标用户ID列表
     * @param requesterId 请求者用户ID（自己查看自己不受隐私设置限制，可为null表示未登录）
     * @return 用户ID -> 在线状态映射，true-在线, false-离线, 不可见的用户不会出现在结果中
     */
    Map<Long, Boolean> batchGetOnlineStatus(List<Long> userIds, Long requesterId);

    /**
     * 心跳（更新 last_active_at）
     *
     * @param userId 用户ID
     */
    void heartbeat(Long userId);

    /**
     * 更新在线状态可见性设置，并清除缓存
     *
     * @param userId 用户ID
     * @param showOnlineStatus 是否显示在线状态：0-隐藏，1-显示
     */
    void updateShowOnlineStatus(Long userId, Integer showOnlineStatus);

    /**
     * 清除指定用户的在线状态可见性缓存
     * 当通过其他途径（如全局设置更新）修改了 showOnlineStatus 时调用
     *
     * @param userId 用户ID
     */
    void invalidateShowOnlineStatusCache(Long userId);
}
