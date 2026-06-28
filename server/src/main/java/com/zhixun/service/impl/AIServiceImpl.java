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

    @Override
    public AIResponseVO generateText(AIWriteRequest request) {
        String apiKey = aiConfig.getApiKey();
        if (apiKey == null || apiKey.isEmpty() || "your-zhipu-api-key".equals(apiKey)) {
            log.warn("ZHIPU_API_KEY not configured, refusing to mock - returning explicit error for mode={}", request.getMode());
            // 不再静默回退到 mock 响应，避免误导用户以为 AI 功能可用
            AIResponseVO vo = new AIResponseVO();
            vo.setContent("AI 服务未配置 API Key");
            vo.setUsage("error: api_key_missing");
            return vo;
        }
        try {
            String url = aiConfig.getBaseUrl() + "/chat/completions";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", aiConfig.getModel());
            body.put("messages", List.of(
                Map.of("role", "system", "content", getSystemPrompt(request.getMode())),
                Map.of("role", "user", "content", buildUserPrompt(request))
            ));
            body.put("max_tokens", 2000);
            body.put("temperature", 0.7);

            String preview = request.getPrompt() != null
                ? request.getPrompt().substring(0, Math.min(50, request.getPrompt().length()))
                : "";
            log.info("Calling ZhiPuAI: model={}, mode={}, prompt={}", aiConfig.getModel(), request.getMode(), preview);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

            Map result = response.getBody();
            if (result == null) {
                throw new RuntimeException("ZhiPuAI returned null response");
            }

            // 智谱免费模型（glm-4.7-flash）会返回 reasoning_content 字段；
            // 我们只关心最终 content 字段。
            List<Map<String, Object>> choices = (List<Map<String, Object>>) result.get("choices");
            if (choices == null || choices.isEmpty()) {
                throw new RuntimeException("ZhiPuAI returned no choices");
            }

            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            String content = (String) message.get("content");
            // glm-4.7-flash 等推理模型会将输出放在 reasoning_content 字段，content 可能为空
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

        } catch (Exception e) {
            // 真实异常：记录日志但不再静默回退 mock，避免误导用户
            log.error("ZhiPuAI API call failed: {}", e.getMessage(), e);
            AIResponseVO vo = new AIResponseVO();
            vo.setContent("AI 服务调用失败：" + e.getMessage() + "。请检查网络或联系管理员。");
            vo.setUsage("error: " + e.getClass().getSimpleName());
            return vo;
        }
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
