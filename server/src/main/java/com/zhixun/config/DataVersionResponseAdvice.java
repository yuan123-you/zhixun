package com.zhixun.config;

import com.zhixun.common.result.R;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 全局响应增强：在所有 API 响应头中添加数据版本号
 * 客户端可据此判断数据是否已变更，自动失效本地缓存
 */
@Slf4j
@ControllerAdvice(basePackages = "com.zhixun.controller")
@RequiredArgsConstructor
public class DataVersionResponseAdvice implements ResponseBodyAdvice<Object> {

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public boolean supports(MethodParameter returnType, Class converterType) {
        // 仅对返回 R 类型的接口添加版本号头
        return MappingJackson2HttpMessageConverter.class.isAssignableFrom(converterType);
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                   Class selectedConverterType, ServerHttpRequest request, ServerHttpResponse response) {
        try {
            long version = RedisConfig.getDataVersion(stringRedisTemplate);
            response.getHeaders().set("X-Data-Version", String.valueOf(version));
        } catch (Exception e) {
            // 获取版本号失败不影响正常响应
            log.debug("获取数据版本号失败: {}", e.getMessage());
        }
        return body;
    }
}
