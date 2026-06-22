package com.zhixun.service;

import com.zhixun.entity.LoginLog;

/**
 * 登录日志服务接口
 */
public interface LoginLogService {

    /**
     * 记录登录日志
     *
     * @param loginLog 登录日志
     */
    void save(LoginLog loginLog);

    /**
     * 异步记录登录日志
     *
     * @param loginLog 登录日志
     */
    void saveAsync(LoginLog loginLog);
}
