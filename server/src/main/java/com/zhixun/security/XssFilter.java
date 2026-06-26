package com.zhixun.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 全局 XSS 过滤器
 * <p>
 * 对请求参数和请求体中的字符串值进行 XSS 过滤：
 * - 普通字段（标题、摘要、昵称等）：HTML 转义
 * - 富文本字段（作品内容等）：由 HtmlWhitelistFilter 在业务层单独处理
 * <p>
 * 注意：富文本内容字段（content）不在此处过滤，而是在 ArticleServiceImpl 中
 * 通过 HtmlWhitelistFilter.filterRichText() 进行白名单过滤
 */
@Slf4j
@Component
@Order(1)
public class XssFilter extends OncePerRequestFilter {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // WebSocket 请求不过滤
        String upgrade = request.getHeader("Upgrade");
        if ("websocket".equalsIgnoreCase(upgrade)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 对 Content-Type 为 application/json 的请求体进行 XSS 过滤
        String contentType = request.getContentType();
        if (contentType != null && contentType.contains("application/json")) {
            XssJsonHttpServletRequestWrapper wrappedRequest = new XssJsonHttpServletRequestWrapper(request);
            filterChain.doFilter(wrappedRequest, response);
        } else {
            // 对表单参数进行 XSS 过滤
            XssHttpServletRequestWrapper wrappedRequest = new XssHttpServletRequestWrapper(request);
            filterChain.doFilter(wrappedRequest, response);
        }
    }

    /**
     * 表单参数 XSS 过滤包装
     */
    static class XssHttpServletRequestWrapper extends HttpServletRequestWrapper {

        public XssHttpServletRequestWrapper(HttpServletRequest request) {
            super(request);
        }

        @Override
        public String getParameter(String name) {
            String value = super.getParameter(name);
            return sanitizeParam(name, value);
        }

        @Override
        public String[] getParameterValues(String name) {
            String[] values = super.getParameterValues(name);
            if (values == null) return null;
            String[] sanitized = new String[values.length];
            for (int i = 0; i < values.length; i++) {
                sanitized[i] = sanitizeParam(name, values[i]);
            }
            return sanitized;
        }

        @Override
        public Map<String, String[]> getParameterMap() {
            Map<String, String[]> originalMap = super.getParameterMap();
            Map<String, String[]> sanitizedMap = new HashMap<>();
            for (Map.Entry<String, String[]> entry : originalMap.entrySet()) {
                String[] values = entry.getValue();
                String[] sanitized = new String[values.length];
                for (int i = 0; i < values.length; i++) {
                    sanitized[i] = sanitizeParam(entry.getKey(), values[i]);
                }
                sanitizedMap.put(entry.getKey(), sanitized);
            }
            return sanitizedMap;
        }
    }

    /**
     * JSON 请求体 XSS 过滤包装
     */
    static class XssJsonHttpServletRequestWrapper extends HttpServletRequestWrapper {

        private byte[] filteredBody;

        public XssJsonHttpServletRequestWrapper(HttpServletRequest request) {
            super(request);
            this.filteredBody = filterJsonBody(request);
        }

        @Override
        public jakarta.servlet.ServletInputStream getInputStream() throws IOException {
            return new jakarta.servlet.ServletInputStream() {
                private final java.io.ByteArrayInputStream byteArrayInputStream =
                        new java.io.ByteArrayInputStream(filteredBody);

                @Override
                public boolean isFinished() {
                    return byteArrayInputStream.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(jakarta.servlet.ReadListener readListener) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public int read() throws IOException {
                    return byteArrayInputStream.read();
                }
            };
        }

        @Override
        public int getContentLength() {
            return filteredBody.length;
        }

        @Override
        public long getContentLengthLong() {
            return filteredBody.length;
        }
    }

    /**
     * 过滤 JSON 请求体中的字符串值
     */
    @SuppressWarnings("unchecked")
    private static byte[] filterJsonBody(HttpServletRequest request) {
        try {
            byte[] body = request.getInputStream().readAllBytes();
            if (body.length == 0) return body;

            Object json = OBJECT_MAPPER.readValue(body, Object.class);
            Object filtered = filterJsonValue(json);
            return OBJECT_MAPPER.writeValueAsBytes(filtered);
        } catch (Exception e) {
            log.warn("XSS JSON 过滤异常，使用原始请求体: {}", e.getMessage());
            try {
                return request.getInputStream().readAllBytes();
            } catch (IOException ex) {
                return new byte[0];
            }
        }
    }

    /**
     * 递归过滤 JSON 值
     */
    @SuppressWarnings("unchecked")
    private static Object filterJsonValue(Object value) {
        if (value == null) return null;
        if (value instanceof String str) {
            return HtmlWhitelistFilter.escapePlainText(str);
        }
        if (value instanceof Map) {
            Map<String, Object> map = new HashMap<>();
            for (Map.Entry<String, Object> entry : ((Map<String, Object>) value).entrySet()) {
                map.put(entry.getKey(), filterJsonValue(entry.getValue()));
            }
            return map;
        }
        if (value instanceof java.util.List) {
            java.util.List<Object> list = new java.util.ArrayList<>();
            for (Object item : (java.util.List<?>) value) {
                list.add(filterJsonValue(item));
            }
            return list;
        }
        return value;
    }

    /**
     * 对请求参数进行 XSS 过滤
     * 富文本字段（content）不做转义，由业务层白名单过滤
     */
    private static String sanitizeParam(String name, String value) {
        if (value == null || value.isEmpty()) return value;
        // content 字段是富文本，不做转义，由业务层 HtmlWhitelistFilter.filterRichText() 处理
        if ("content".equalsIgnoreCase(name)) {
            return value;
        }
        return HtmlWhitelistFilter.escapePlainText(value);
    }
}
