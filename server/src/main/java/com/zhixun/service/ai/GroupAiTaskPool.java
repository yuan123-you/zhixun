package com.zhixun.service.ai;

import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/** Bounded, managed group-AI resources without becoming the application's default @Async executor. */
public class GroupAiTaskPool implements GroupAiExecutor, AutoCloseable {
    private final ThreadPoolTaskExecutor pool;

    public GroupAiTaskPool(int coreSize, int maxSize, int queueCapacity) {
        if (coreSize < 1 || maxSize < coreSize || queueCapacity < 0) {
            throw new IllegalArgumentException("群组 AI 线程池配置无效");
        }
        pool = new ThreadPoolTaskExecutor();
        pool.setCorePoolSize(coreSize);
        pool.setMaxPoolSize(maxSize);
        pool.setQueueCapacity(queueCapacity);
        pool.setThreadNamePrefix("group-ai-");
        pool.initialize();
    }

    @Override public void execute(Runnable task) { pool.execute(task); }
    @Override public void close() { pool.shutdown(); }
}
