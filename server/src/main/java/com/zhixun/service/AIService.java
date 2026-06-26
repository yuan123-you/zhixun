package com.zhixun.service;

import com.zhixun.dto.ai.AIWriteRequest;
import com.zhixun.dto.ai.AIGenerateImageRequest;
import com.zhixun.vo.AIResponseVO;

public interface AIService {
    AIResponseVO generateText(AIWriteRequest request);
    AIResponseVO generateSummary(String content);
    AIResponseVO generateImage(AIGenerateImageRequest request);
    AIResponseVO reviewContent(String content);
}