package com.zhixun.controller;

import com.zhixun.common.result.R;
import com.zhixun.dto.ai.AIWriteRequest;
import com.zhixun.dto.ai.AIGenerateImageRequest;
import com.zhixun.service.AIService;
import com.zhixun.vo.AIResponseVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/ai")
@RequiredArgsConstructor
public class AIController {
    private final AIService aiService;

    @PostMapping("/write")
    @PreAuthorize("isAuthenticated()")
    public R<AIResponseVO> write(@Valid @RequestBody AIWriteRequest request) {
        return R.ok(aiService.generateText(request));
    }

    @PostMapping("/summarize")
    @PreAuthorize("isAuthenticated()")
    public R<AIResponseVO> summarize(@RequestBody String content) {
        return R.ok(aiService.generateSummary(content));
    }

    @PostMapping("/image")
    @PreAuthorize("isAuthenticated()")
    public R<AIResponseVO> generateImage(@Valid @RequestBody AIGenerateImageRequest request) {
        return R.ok(aiService.generateImage(request));
    }

    @PostMapping("/review")
    @PreAuthorize("isAuthenticated()")
    public R<AIResponseVO> review(@RequestBody String content) {
        return R.ok(aiService.reviewContent(content));
    }
}