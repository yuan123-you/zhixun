package com.zhixun.controller;

import com.zhixun.common.exception.BusinessException;
import com.zhixun.common.exception.GlobalExceptionHandler;
import com.zhixun.dto.ai.AIWriteRequest;
import com.zhixun.dto.ai.AIGenerateImageRequest;
import com.zhixun.service.AIService;
import com.zhixun.vo.AIResponseVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AiResultContractTest {
    private AIResponseVO result(String content, String usage) {
        AIResponseVO result = new AIResponseVO(); result.setContent(content); result.setUsage(usage); return result;
    }

    @ParameterizedTest @ValueSource(strings = {"write", "summarize", "review", "image"})
    void explicitProviderErrorsCannotBeReportedAsSuccessfulHttpResults(String endpoint) {
        AIService provider = mock(AIService.class);
        var error = result("Provider failed", "error: upstream_unavailable");
        var controller = new AIController(provider);
        AIWriteRequest write = new AIWriteRequest(); write.setPrompt("Question");
        AIGenerateImageRequest image = new AIGenerateImageRequest(); image.setPrompt("Example");
        BusinessException failure = assertThrows(BusinessException.class, () -> {
            switch (endpoint) {
                case "write" -> { when(provider.generateText(any())).thenReturn(error); controller.write(write); }
                case "summarize" -> { when(provider.generateSummary(anyString())).thenReturn(error); controller.summarize("Content"); }
                case "review" -> { when(provider.reviewContent(anyString())).thenReturn(error); controller.review("Content"); }
                case "image" -> { when(provider.generateImage(any())).thenReturn(error); controller.generateImage(image); }
                default -> throw new AssertionError(endpoint);
            }
        });
        assertEquals(503, failure.getCode());
    }

    @Test void providerFailureUsesTheExisting503HttpContract() throws Exception {
        AIService provider = mock(AIService.class);
        when(provider.generateText(any())).thenReturn(result("Provider failed", "error: api_key_missing"));
        var mvc = MockMvcBuilders.standaloneSetup(new AIController(provider))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        mvc.perform(post("/v1/ai/write").contentType(MediaType.APPLICATION_JSON).content("{\"prompt\":\"Question\"}"))
                .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.code").value(503));
    }

    @Test void validResponsePreservesContentAndUsage() {
        AIService provider = mock(AIService.class);
        var expected = result("Valid final answer", "tokens=5");
        when(provider.generateText(any())).thenReturn(expected);
        AIWriteRequest request = new AIWriteRequest(); request.setPrompt("Question");
        var response = new AIController(provider).write(request);
        assertEquals(expected, response.getData());
    }
}
