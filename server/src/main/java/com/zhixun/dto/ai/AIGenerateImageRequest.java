package com.zhixun.dto.ai;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AIGenerateImageRequest {
    @NotBlank(message = "鍥剧墖鎻忚堪涓嶈兘涓虹┖")
    private String prompt;
    private String style;
    private String size = "1024x1024";
}