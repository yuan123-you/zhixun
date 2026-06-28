package com.zhixun.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.List;

/**
 * 跨域配置
 */
@Configuration
public class CorsConfig {

    // 端口来源：docs/PORTS.md
    // - Backend SERVER_PORT=8080
    // - Client  CLIENT_PORT=3500 / 5173（Vite dev 默认端口）
    // - Admin   ADMIN_PORT=3001
    @Value("${cors.allowed-origins:http://localhost:3500,http://localhost:5173,http://localhost:3001,http://localhost:8080,https://glint.novo.ccwu.cc}")
    private String allowedOrigins;

    /**
     * 提供 CorsConfigurationSource，供 Spring Security 的 .cors() 使用
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // 仅允许配置的来源
        List<String> origins = Arrays.asList(allowedOrigins.split(","));
        config.setAllowedOrigins(origins);
        // 允许的请求头（包含 CSRF 防护所需的 X-XSRF-TOKEN 与通用跨域头）
        config.setAllowedHeaders(Arrays.asList(
                "Authorization", "Content-Type", "X-XSRF-TOKEN", "X-Requested-With"
        ));
        // 允许所有请求方法
        config.addAllowedMethod("*");
        // 允许携带凭证
        config.setAllowCredentials(true);
        // 预检请求缓存时间（秒）
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // 对所有路径生效
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    /**
     * 兼容老代码：保留 CorsFilter Bean
     */
    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = (UrlBasedCorsConfigurationSource) corsConfigurationSource();
        return new CorsFilter(source);
    }
}
