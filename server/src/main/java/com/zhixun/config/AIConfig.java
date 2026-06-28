package com.zhixun.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "zhixun.ai")
public class AIConfig {

    /** ZhiPuAI API Key */
    private String apiKey = "";

    /** ZhiPuAI base URL */
    private String baseUrl = "https://open.bigmodel.cn/api/paas/v4";

    /** ZhiPuAI 免费模型 glm-4.7-flash（300亿参数MoE） */
    private String model = "glm-4.7-flash";
}
