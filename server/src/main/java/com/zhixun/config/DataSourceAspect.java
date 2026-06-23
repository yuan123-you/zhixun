package com.zhixun.config;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 数据源切换切面
 * 根据方法上的 @Master / @Slave 注解自动切换数据源
 * 无注解时：写操作走主库，读操作走从库
 * 支持从库故障自动回退到主库
 */
@Slf4j
@Aspect
@Component
@Order(-1) // 确保在事务切面之前执行
public class DataSourceAspect {

    /** 读操作计数 */
    private static final AtomicLong READ_COUNT = new AtomicLong(0);

    /** 写操作计数 */
    private static final AtomicLong WRITE_COUNT = new AtomicLong(0);

    /** 从库回退计数 */
    private static final AtomicLong SLAVE_FALLBACK_COUNT = new AtomicLong(0);

    /**
     * 获取读操作计数
     */
    public static long getReadCount() {
        return READ_COUNT.get();
    }

    /**
     * 获取写操作计数
     */
    public static long getWriteCount() {
        return WRITE_COUNT.get();
    }

    /**
     * 获取从库回退计数
     */
    public static long getSlaveFallbackCount() {
        return SLAVE_FALLBACK_COUNT.get();
    }

    /**
     * 重置计数
     */
    public static void resetCounts() {
        READ_COUNT.set(0);
        WRITE_COUNT.set(0);
        SLAVE_FALLBACK_COUNT.set(0);
    }

    /**
     * 环绕通知，根据注解切换数据源
     */
    @Around("execution(* com.zhixun.service..*.*(..))")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();

        boolean useSlave = false;

        try {
            // 1. 检查方法上的注解
            if (method.isAnnotationPresent(Master.class)) {
                DataSourceContextHolder.master();
                log.debug("方法 {} 标记 @Master，切换到主库", method.getName());
                WRITE_COUNT.incrementAndGet();
            } else if (method.isAnnotationPresent(Slave.class)) {
                // 显式标记 @Slave，检查从库是否可用
                if (DynamicDataSourceConfig.isSlaveAvailable()) {
                    DataSourceContextHolder.slave();
                    useSlave = true;
                    log.debug("方法 {} 标记 @Slave，切换到从库", method.getName());
                } else {
                    DataSourceContextHolder.forceMaster();
                    SLAVE_FALLBACK_COUNT.incrementAndGet();
                    log.warn("方法 {} 标记 @Slave，但从库不可用，回退到主库", method.getName());
                }
                READ_COUNT.incrementAndGet();
            } else {
                // 2. 无注解时，根据方法名前缀判断读写
                String methodName = method.getName();
                if (isWriteOperation(methodName)) {
                    DataSourceContextHolder.master();
                    log.debug("方法 {} 为写操作，自动切换到主库", method.getName());
                    WRITE_COUNT.incrementAndGet();
                } else {
                    // 读操作：检查从库是否可用
                    if (DynamicDataSourceConfig.isSlaveAvailable()) {
                        DataSourceContextHolder.slave();
                        useSlave = true;
                        log.debug("方法 {} 为读操作，自动切换到从库", method.getName());
                    } else {
                        DataSourceContextHolder.forceMaster();
                        SLAVE_FALLBACK_COUNT.incrementAndGet();
                        log.warn("方法 {} 为读操作，但从库不可用，回退到主库", method.getName());
                    }
                    READ_COUNT.incrementAndGet();
                }
            }

            return point.proceed();
        } catch (Exception e) {
            // 3. 如果是从库操作失败，尝试回退到主库重试
            if (useSlave && isConnectionException(e)) {
                log.warn("从库操作失败，尝试回退到主库重试: {}", e.getMessage());
                DynamicDataSourceConfig.markSlaveUnavailable();

                try {
                    DataSourceContextHolder.forceMaster();
                    SLAVE_FALLBACK_COUNT.incrementAndGet();
                    return point.proceed();
                } catch (Exception retryEx) {
                    log.error("主库重试也失败: {}", retryEx.getMessage());
                    throw retryEx;
                }
            }
            throw e;
        } finally {
            // 4. 方法执行完毕后清除数据源标识，防止线程复用导致的数据源污染
            DataSourceContextHolder.clear();

            // 5. 如果从库标记为不可用，定期尝试恢复
            if (!DynamicDataSourceConfig.isSlaveAvailable()) {
                DynamicDataSourceConfig.tryRecoverSlave();
            }
        }
    }

    /**
     * 判断是否为写操作
     * 根据方法名前缀判断：insert/update/delete/save/remove/add/create/edit/modify/write/batch
     *
     * @param methodName 方法名
     * @return true-写操作
     */
    private boolean isWriteOperation(String methodName) {
        return methodName.startsWith("insert")
                || methodName.startsWith("update")
                || methodName.startsWith("delete")
                || methodName.startsWith("save")
                || methodName.startsWith("remove")
                || methodName.startsWith("add")
                || methodName.startsWith("create")
                || methodName.startsWith("edit")
                || methodName.startsWith("modify")
                || methodName.startsWith("write")
                || methodName.startsWith("batch")
                || methodName.startsWith("audit")
                || methodName.startsWith("send")
                || methodName.startsWith("publish")
                || methodName.startsWith("upload")
                || methodName.startsWith("login")
                || methodName.startsWith("register")
                || methodName.startsWith("follow")
                || methodName.startsWith("unfollow")
                || methodName.startsWith("like")
                || methodName.startsWith("unlike")
                || methodName.startsWith("collect")
                || methodName.startsWith("uncollect")
                || methodName.startsWith("evict");
    }

    /**
     * 判断是否为数据库连接异常
     */
    private boolean isConnectionException(Exception e) {
        String message = e.getMessage();
        if (message == null) return false;
        String lowerMessage = message.toLowerCase();
        return lowerMessage.contains("connection") 
                || lowerMessage.contains("timeout")
                || lowerMessage.contains("refused")
                || lowerMessage.contains("reset")
                || lowerMessage.contains("broken pipe")
                || lowerMessage.contains("no route to host")
                || lowerMessage.contains("connect timed out");
    }
}
