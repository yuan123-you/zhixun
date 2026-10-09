package com.zhixun.service.impl;

import com.zhixun.common.exception.BusinessException;
import com.zhixun.config.AIConfig;
import com.zhixun.dto.ai.AIWriteRequest;
import com.zhixun.dto.ai.AIGenerateImageRequest;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AiProviderFinalResultTest {
    private AIServiceImpl textProvider(Map<String, Object> message) {
        AIConfig config = new AIConfig(); config.setApiKey("test-key-not-real");
        config.setBaseUrl("https://provider.invalid/mock"); config.setModel("test-model"); config.setFallbackModel("");
        AIServiceImpl service = new AIServiceImpl(config);
        RestTemplate client = mock(RestTemplate.class);
        ReflectionTestUtils.setField(service, "restTemplate", client);
        Map response = Map.of("choices", List.of(Map.of("message", message)));
        when(client.postForEntity(anyString(), any(HttpEntity.class), eq(Map.class))).thenReturn(ResponseEntity.ok(response));
        return service;
    }

    @Test void reasoningWithoutAFinalAnswerIsNotSuccessfulGeneratedText() {
        var provider = textProvider(Map.of("reasoning_content", "internal-draft-only"));
        AIWriteRequest request = new AIWriteRequest(); request.setPrompt("Question"); request.setMode("chat");
        var result = provider.generateText(request);
        assertTrue(result.getUsage().startsWith("error:"));
        assertFalse(result.getContent().contains("internal-draft-only"));
    }

    @Test void finalAnswerIsReturnedInsteadOfReasoning() {
        var provider = textProvider(Map.of("content", "Valid final", "reasoning_content", "internal-draft-only"));
        AIWriteRequest request = new AIWriteRequest(); request.setPrompt("Question"); request.setMode("chat");
        assertEquals("Valid final", provider.generateText(request).getContent());
    }

    @Test void unimplementedImageEndpointCannotClaimThatARealImageWasGenerated() {
        AIGenerateImageRequest request = new AIGenerateImageRequest(); request.setPrompt("Example image");
        var failure = assertThrows(BusinessException.class, () -> new AIServiceImpl(new AIConfig()).generateImage(request));
        assertEquals(503, failure.getCode());
    }
}
