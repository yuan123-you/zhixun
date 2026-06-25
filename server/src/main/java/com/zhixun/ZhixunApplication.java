package com.zhixun;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 知讯 - 内容资讯发布与管理系统 启动类
 */
@EnableAsync
@EnableScheduling
@SpringBootApplication
public class ZhixunApplication {

    public static void main(String[] args) {
        SpringApplication.run(ZhixunApplication.class, args);
    }
}
