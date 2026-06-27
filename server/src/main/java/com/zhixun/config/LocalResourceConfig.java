package com.zhixun.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 本地文件存储的静态资源映射
 * 始终注册，作为 MinIO 不可用时的降级方案
 */
@Slf4j
@Configuration
public class LocalResourceConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 将 /uploads/** 请求映射到本地 ./uploads/ 目录
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:./uploads/");
        log.info("已配置本地文件存储静态资源映射: /uploads/** -> ./uploads/");
    }
}
