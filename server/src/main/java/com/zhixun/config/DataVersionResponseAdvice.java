package com.zhixun.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * 全局响应增强：在所有 API 响应头中添加数据版本号
 * 客户端可据此判断数据是否已变更，自动失效本地缓存
 */
@RestControllerAdvice
public class DataVersionResponseAdvice implements ResponseBodyAdvice<Object> {

    @Value("${app.data-version:1}")
    private String dataVersion;

    @Override
    public boolean supports(MethodParameter returnType, Class converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType,
            MediaType selectedContentType, Class selectedConverterType,
            ServerHttpRequest request, ServerHttpResponse response) {
        response.getHeaders().set("x-data-version", dataVersion);
        return body;
    }
}
