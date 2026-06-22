package com.zhixun.service.impl;

import com.zhixun.entity.LoginLog;
import com.zhixun.mapper.LoginLogMapper;
import com.zhixun.service.LoginLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 登录日志服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginLogServiceImpl implements LoginLogService {

    private final LoginLogMapper loginLogMapper;

    @Override
    public void save(LoginLog loginLog) {
        loginLogMapper.insert(loginLog);
    }

    @Async
    @Override
    public void saveAsync(LoginLog loginLog) {
        try {
            loginLogMapper.insert(loginLog);
        } catch (Exception e) {
            log.error("异步保存登录日志失败: {}", e.getMessage());
        }
    }
}
