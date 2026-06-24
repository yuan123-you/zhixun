package com.zhixun.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 默认用户数据初始化
 * 默认用户初始化已移除，请通过注册接口创建用户
 */
@Component
@Order(0)
public class DefaultUserDataInitializer implements CommandLineRunner {

    @Override
    public void run(String... args) {
        // 默认用户初始化已移除，请通过注册接口创建用户
    }

}
