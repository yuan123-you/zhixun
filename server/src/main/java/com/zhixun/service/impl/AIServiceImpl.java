package com.zhixun.service.impl;

import com.zhixun.config.AIConfig;
import com.zhixun.dto.ai.AIWriteRequest;
import com.zhixun.dto.ai.AIGenerateImageRequest;
import com.zhixun.service.AIService;
import com.zhixun.vo.AIResponseVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIServiceImpl implements AIService {

    private final AIConfig aiConfig;
    /**
     * 智谱 AI RestTemplate：配置连接/读取超时，避免被默认无限超时阻塞请求线程
     */
    private final RestTemplate restTemplate = createRestTemplate();

    private static RestTemplate createRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) Duration.ofSeconds(15).toMillis());
        factory.setReadTimeout((int) Duration.ofSeconds(120).toMillis());
        return new RestTemplate(factory);
    }

    /** 最大重试次数（429 限流时） */
    private static final int MAX_RETRIES = 3;
    /** 重试基础延迟（毫秒），实际延迟 = base * 2^attempt */
    private static final long RETRY_BASE_DELAY_MS = 2000;

    @Override
    public AIResponseVO generateText(AIWriteRequest request) {
        String apiKey = aiConfig.getApiKey();
        if (apiKey == null || apiKey.isEmpty() || "your-zhipu-api-key".equals(apiKey)) {
            log.warn("ZHIPU_API_KEY not configured, refusing to mock - returning explicit error for mode={}", request.getMode());
            AIResponseVO vo = new AIResponseVO();
            vo.setContent("AI 服务未配置 API Key，请联系管理员");
            vo.setUsage("error: api_key_missing");
            return vo;
        }

        String url = aiConfig.getBaseUrl() + "/chat/completions";
        String preview = request.getPrompt() != null
            ? request.getPrompt().substring(0, Math.min(50, request.getPrompt().length()))
            : "";
        String primaryModel = aiConfig.getModel();
        String fallbackModel = aiConfig.getFallbackModel();
        log.info("Calling ZhiPuAI: model={}, fallback={}, mode={}, prompt={}", primaryModel, fallbackModel, request.getMode(), preview);

        // 1. 先尝试主模型（含重试）
        TryResult primaryResult = tryWithModel(url, apiKey, request, primaryModel);
        if (primaryResult.isSuccess()) {
            return primaryResult.result;
        }

        // 2. 主模型全部重试失败且为限流错误，尝试降级模型
        Exception lastException = primaryResult.exception;
        if (lastException instanceof RateLimitException && fallbackModel != null && !fallbackModel.isEmpty() && !fallbackModel.equals(primaryModel)) {
            log.warn("主模型 {} 限流，降级到备用模型 {}", primaryModel, fallbackModel);
            TryResult fallbackResult = tryWithModel(url, apiKey, request, fallbackModel);
            if (fallbackResult.isSuccess()) {
                return fallbackResult.result;
            }
            lastException = fallbackResult.exception;
        }

        // 3. 所有尝试均失败
        log.error("ZhiPuAI API call failed for all models: {}", lastException != null ? lastException.getMessage() : "unknown");
        AIResponseVO vo = new AIResponseVO();
        String userMsg = (lastException instanceof RateLimitException)
            ? "AI 服务当前请求过于频繁，请稍后再试"
            : "AI 服务暂时不可用，请稍后再试";
        vo.setContent(userMsg);
        vo.setUsage("error: " + (lastException != null ? lastException.getClass().getSimpleName() : "unknown"));
        return vo;
    }

    /**
     * 线程安全的调用结果：成功时持有 result，失败时持有 exception
     */
    private static class TryResult {
        final AIResponseVO result;
        final Exception exception;
        TryResult(AIResponseVO result) { this.result = result; this.exception = null; }
        TryResult(Exception exception) { this.result = null; this.exception = exception; }
        boolean isSuccess() { return result != null; }
    }

    private TryResult tryWithModel(String url, String apiKey, AIWriteRequest request, String model) {
        Exception lastException = null;
        for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
            try {
                return new TryResult(doCallZhiPuAI(url, apiKey, request, model));
            } catch (RateLimitException e) {
                lastException = e;
                if (attempt < MAX_RETRIES) {
                    long delay = RETRY_BASE_DELAY_MS * (1L << attempt);
                    log.warn("ZhiPuAI [{}] 429 限流, 第{}次重试, 等待{}ms: {}", model, attempt + 1, delay, e.getMessage());
                    try { Thread.sleep(delay); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
                }
            } catch (Exception e) {
                lastException = e;
                break; // 非限流异常不重试
            }
        }
        return new TryResult(lastException);
    }

    /**
     * 单次调用智谱 API，限流时抛出 RateLimitException 以便外层重试
     */
    @SuppressWarnings("unchecked")
    private AIResponseVO doCallZhiPuAI(String url, String apiKey, AIWriteRequest request, String model) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("messages", List.of(
            Map.of("role", "system", "content", getSystemPrompt(request.getMode())),
            Map.of("role", "user", "content", buildUserPrompt(request))
        ));
        body.put("max_tokens", 2000);
        body.put("temperature", 0.7);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            Map result = response.getBody();
            if (result == null) {
                throw new RuntimeException("ZhiPuAI returned null response");
            }

            // 检查 API 级别的错误（部分 200 响应也带 error 字段）
            Map<String, Object> error = (Map<String, Object>) result.get("error");
            if (error != null) {
                String code = String.valueOf(error.get("code"));
                String msg = String.valueOf(error.get("message"));
                // 1302 = 速率限制
                if ("1302".equals(code)) {
                    throw new RateLimitException("ZhiPuAI rate limit: " + msg);
                }
                throw new RuntimeException("ZhiPuAI error [" + code + "]: " + msg);
            }

            List<Map<String, Object>> choices = (List<Map<String, Object>>) result.get("choices");
            if (choices == null || choices.isEmpty()) {
                throw new RuntimeException("ZhiPuAI returned no choices");
            }

            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            String content = (String) message.get("content");
            if (content == null || content.isEmpty()) {
                content = (String) message.get("reasoning_content");
            }
            if (content == null || content.isEmpty()) {
                throw new RuntimeException("ZhiPuAI returned empty content and reasoning_content");
            }
            Map<String, Object> usage = (Map<String, Object>) result.get("usage");

            String usageInfo = usage != null
                ? "prompt_tokens=" + usage.get("prompt_tokens") +
                  ", completion_tokens=" + usage.get("completion_tokens") +
                  ", total_tokens=" + usage.get("total_tokens")
                : "";

            log.info("ZhiPuAI response: tokens={}", usageInfo);
            return buildResponse(content, usageInfo);

        } catch (org.springframework.web.client.HttpClientErrorException e) {
            if (e.getStatusCode().value() == 429) {
                throw new RateLimitException("HTTP 429: " + e.getResponseBodyAsString());
            }
            throw e;
        }
    }

    /** 内部异常：标识智谱 API 的速率限制，触发重试逻辑 */
    private static class RateLimitException extends Exception {
        RateLimitException(String message) { super(message); }
    }

    @Override
    public AIResponseVO generateSummary(String content) {
        if (content == null || content.isEmpty()) {
            AIResponseVO vo = new AIResponseVO();
            vo.setContent("content is empty");
            vo.setUsage("empty input");
            return vo;
        }
        if (content.length() < 100) {
            AIResponseVO vo = new AIResponseVO();
            vo.setContent(content.substring(0, Math.min(200, content.length())));
            vo.setUsage("local truncation");
            return vo;
        }
        AIWriteRequest req = new AIWriteRequest();
        req.setPrompt(content);
        req.setMode("summarize");
        return generateText(req);
    }

    @Override
    public AIResponseVO generateImage(AIGenerateImageRequest request) {
        AIResponseVO vo = new AIResponseVO();
        vo.setContent("[AI image generation: " + request.getPrompt() + "]");
        vo.setUsage("ZhiPuAI CogView is available for image generation");
        log.info("Image gen requested via ZhiPuAI: {}", request.getPrompt());
        return vo;
    }

    @Override
    public AIResponseVO reviewContent(String content) {
        if (content == null || content.isEmpty()) {
            AIResponseVO vo = new AIResponseVO();
            vo.setContent("[passed] content is empty");
            vo.setUsage("empty input");
            return vo;
        }
        AIWriteRequest req = new AIWriteRequest();
        req.setPrompt(content);
        req.setMode("review");
        return generateText(req);
    }

    /**
     * Mock 响应生成器（已废弃：当 ZHIPU_API_KEY 未配置时改为显式返回错误，不再静默回退 mock）。
     * 保留此方法以便将来单元测试或离线开发场景使用。
     */
    @Deprecated
    private AIResponseVO mockTextResponse(AIWriteRequest request) {
        AIResponseVO vo = new AIResponseVO();
        String prompt = request.getPrompt();
        if (prompt == null) prompt = "";
        switch (request.getMode()) {
            case "summarize":
                vo.setContent("[AI Summary] " + (prompt.length() > 100 ? prompt.substring(0, 100) + "..." : prompt));
                break;
            case "polish":
                vo.setContent("[AI Polished] " + prompt);
                break;
            case "translate":
                vo.setContent("[AI Translated] " + prompt);
                break;
            case "review":
                StringBuilder sb = new StringBuilder();
                if (prompt.contains("\u654f\u611f") || prompt.contains("\u8fdd\u6cd5"))
                    sb.append("\u26a0\ufe0f \u68c0\u6d4b\u5230\u654f\u611f\u5185\u5bb9\u98ce\u9669; ");
                if (prompt.length() < 20)
                    sb.append("\ud83d\udcdd \u5185\u5bb9\u8fc7\u77ed\uff0c\u5efa\u8bae\u4e30\u5bcc; ");
                if (sb.length() == 0)
                    sb.append("\u2705 \u5185\u5bb9\u5ba1\u6838\u901a\u8fc7");
                vo.setContent(sb.toString());
                break;
            default:
                vo.setContent("[AI Expanded] " + prompt +
                    "\n\n(\u6b64\u5185\u5bb9\u7531AI\u751f\u6210\uff0c\u914d\u7f6e\u667a\u8c31API Key\u4ee5\u83b7\u5f97\u5b8c\u6574\u4f53\u9a8c)");
                break;
        }
        vo.setUsage("mock (configure zhipu API key in application.yml)");
        return vo;
    }

    private String getSystemPrompt(String mode) {
        switch (mode) {
            case "chat":
                return "你是知讯群组的AI助手。请用简洁、友好的中文回答用户的问题。" +
                       "回答要准确、有条理，必要时可以使用列表或分点说明。" +
                       "如果不确定答案，请诚实说明。";
            case "summarize":
                return "\u4f60\u662f\u4e00\u4e2a\u4e13\u4e1a\u7684\u6587\u672c\u6458\u8981\u52a9\u624b\u3002" +
                       "\u8bf7\u7528\u7b80\u6d01\u7684\u8bed\u8a00\u603b\u7ed3\u4ee5\u4e0b\u5185\u5bb9\uff0c" +
                       "\u4fdd\u7559\u6838\u5fc3\u89c2\u70b9\uff0c\u63a7\u5236\u5728200\u5b57\u4ee5\u5185\u3002";
            case "polish":
                return "\u4f60\u662f\u4e00\u4e2a\u4e13\u4e1a\u7684\u6587\u5b57\u6da6\u8272\u52a9\u624b\u3002" +
                       "\u8bf7\u5728\u4e0d\u6539\u53d8\u539f\u610f\u7684\u57fa\u7840\u4e0a\uff0c" +
                       "\u4f18\u5316\u6587\u5b57\u8868\u8fbe\uff0c\u4f7f\u5176\u66f4\u52a0\u6d41\u7545\u4f18\u7f8e\u3002";
            case "translate":
                return "\u4f60\u662f\u4e00\u4e2a\u4e13\u4e1a\u7684\u7ffb\u8bd1\u52a9\u624b\u3002" +
                       "\u8bf7\u5c06\u4ee5\u4e0b\u5185\u5bb9\u7ffb\u8bd1\u6210\u4e2d\u6587\u3002";
            case "review":
                return "\u4f60\u662f\u4e00\u4e2a\u4e13\u4e1a\u7684\u5185\u5bb9\u5ba1\u6838\u52a9\u624b\u3002" +
                       "\u8bf7\u68c0\u67e5\u4ee5\u4e0b\u5185\u5bb9\u662f\u5426\u5305\u542b\u654f\u611f\u8bcd\u6c47\u3001" +
                       "\u8fdd\u89c4\u5185\u5bb9\u6216\u8d28\u91cf\u95ee\u9898\uff0c\u5e76\u7ed9\u51fa\u5ba1\u6838\u610f\u89c1\u3002";
            default:
                return "\u4f60\u662f\u4e00\u4e2a\u4e13\u4e1a\u7684\u5185\u5bb9\u521b\u4f5c\u52a9\u624b\u3002" +
                       "\u8bf7\u57fa\u4e8e\u7528\u6237\u7684\u63d0\u793a\uff0c\u6269\u5199\u548c\u4e30\u5bcc\u5185\u5bb9\uff0c" +
                       "\u4fdd\u6301\u903b\u8f91\u6e05\u6670\u3001\u884c\u6587\u6d41\u7545\u3002";
        }
    }

    private String buildUserPrompt(AIWriteRequest request) {
        if (request.getContext() != null && !request.getContext().isEmpty()) {
            return "Context: " + request.getContext() + "\n\nUser input: " + request.getPrompt();
        }
        return request.getPrompt();
    }

    private AIResponseVO buildResponse(String content, String usage) {
        AIResponseVO vo = new AIResponseVO();
        vo.setContent(content);
        vo.setUsage(usage);
        return vo;
    }
}
