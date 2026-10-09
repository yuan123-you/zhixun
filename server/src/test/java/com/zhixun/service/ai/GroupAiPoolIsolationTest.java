package com.zhixun.service.ai;

import com.zhixun.config.GroupAiTaskConfig;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import static org.junit.jupiter.api.Assertions.*;

class GroupAiPoolIsolationTest {
    @Test void aiPoolDoesNotReplaceTheApplicationsGeneralAsyncExecutor() {
        try (var context = new AnnotationConfigApplicationContext(GroupAiTaskConfig.class, TaskExecutionAutoConfiguration.class)) {
            Object dedicated = context.getBean(GroupAiExecutor.class);
            assertFalse(dedicated instanceof Executor);
            assertTrue(context.getBean("applicationTaskExecutor") instanceof Executor);
        }
    }
    @Test void saturatedDedicatedPoolRejectsWithoutRunningWorkInTheRequestThread() throws Exception {
        CountDownLatch started = new CountDownLatch(1), release = new CountDownLatch(1);
        try (GroupAiTaskPool pool = new GroupAiTaskPool(1, 1, 0)) {
            try {
                pool.execute(() -> {
                    started.countDown();
                    try { release.await(5, TimeUnit.SECONDS); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                });
                assertTrue(started.await(5, TimeUnit.SECONDS));
                assertThrows(RejectedExecutionException.class, () -> pool.execute(() -> fail("must not run")));
            } finally { release.countDown(); }
        }
    }
    @ParameterizedTest @CsvSource({"0,1,0", "2,1,2", "1,1,-1"})
    void invalidPoolConfigurationIsRejected(int core, int max, int queue) {
        assertThrows(IllegalArgumentException.class, () -> new GroupAiTaskPool(core, max, queue));
    }
}
