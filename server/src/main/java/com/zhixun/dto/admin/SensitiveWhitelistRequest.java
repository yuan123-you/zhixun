package com.zhixun.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 敏感词白名单添加请求
 */
@Data
public class SensitiveWhitelistRequest {

    /** 白名单词汇 */
    @NotBlank(message = "白名单词汇不能为空")
    @Size(max = 100, message = "白名单词汇最长100个字符")
    private String word;
}
