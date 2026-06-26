package com.zhixun.dto.ai;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AIWriteRequest {
    @NotBlank(message = "提示词不能为空")
    private String prompt;
    private String context;
    private String mode = "expand";
}
