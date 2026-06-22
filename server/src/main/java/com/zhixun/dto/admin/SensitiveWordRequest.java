package com.zhixun.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 敏感词添加请求
 */
@Data
public class SensitiveWordRequest {

    /** 敏感词内容 */
    @NotBlank(message = "敏感词不能为空")
    @Size(max = 50, message = "敏感词最长50个字符")
    private String word;

    /** 敏感词级别：1-警告，2-禁用 */
    @NotNull(message = "级别不能为空")
    private Integer level;
}
