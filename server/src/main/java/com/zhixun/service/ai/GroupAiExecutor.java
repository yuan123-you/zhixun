package com.zhixun.service.ai;

/** Dedicated execution seam, deliberately not a general Spring/Java Executor bean. */
@FunctionalInterface
public interface GroupAiExecutor {
    void execute(Runnable task);
}
