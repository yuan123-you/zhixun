package com.zhixun.config;

import com.zhixun.service.ai.GroupAiTaskPool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GroupAiTaskConfig {
    @Bean(destroyMethod = "close")
    public GroupAiTaskPool groupAiExecutor(
            @Value("${zhixun.ai.group-tasks.core-size:2}") int coreSize,
            @Value("${zhixun.ai.group-tasks.max-size:4}") int maxSize,
            @Value("${zhixun.ai.group-tasks.queue-capacity:32}") int queueCapacity) {
        return new GroupAiTaskPool(coreSize, maxSize, queueCapacity);
    }
}
