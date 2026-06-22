package com.zhixun;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 智讯 - 互联网小单售后工单系统 启动类
 */
@EnableAsync
@EnableScheduling
@SpringBootApplication
public class ZhixunApplication {

    public static void main(String[] args) {
        SpringApplication.run(ZhixunApplication.class, args);
    }
}
