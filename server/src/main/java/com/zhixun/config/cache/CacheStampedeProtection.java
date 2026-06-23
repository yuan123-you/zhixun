package com.zhixun.config.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

/**
 * 缓存击穿防护 - 单飞模式（SingleFlight）
 * <p>
 * 当缓存失效时，确保对同一个 key 只有一个请求去查询数据库，
 * 其他并发请求等待该结果，避免大量请求同时穿透到数据库。
 * <p>
 * 实现原理：使用 ConcurrentHashMap + CompletableFuture
 * - 第一个请求创建 Future 并开始加载数据
 * - 后续请求发现已有相同 key 的 Future，直接等待结果
 * - 加载完成后所有等待的请求共享同一个结果
 */
@Slf4j
@Component
public class CacheStampedeProtection {

    /** 正在加载的 Future 映射 */
    private final Map<String, CompletableFuture<?>> inflightRequests = new ConcurrentHashMap<>();

    /**
     * 单飞执行：对同一个 key，只有一个请求会实际执行 loader，其他请求等待结果
     *
     * @param key    缓存键（用于标识同一请求）
     * @param loader 数据加载器
     * @param <T>    返回值类型
     * @return 加载结果
     */
    @SuppressWarnings("unchecked")
    public <T> T execute(String key, Supplier<T> loader) {
        // 尝试创建新的 Future
        CompletableFuture<T> future = new CompletableFuture<>();
        CompletableFuture<?> existing = inflightRequests.putIfAbsent(key, future);

        if (existing == null) {
            // 当前请求是第一个，执行加载
            log.debug("单飞: 获取执行权, key={}", key);
            try {
                T result = loader.get();
                future.complete(result);
                return result;
            } catch (Exception e) {
                future.completeExceptionally(e);
                throw new RuntimeException("单飞加载失败: key=" + key, e);
            } finally {
                inflightRequests.remove(key, future);
            }
        } else {
            // 已有请求在加载，等待结果
            log.debug("单飞: 等待已有请求结果, key={}", key);
            try {
                return (T) existing.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("等待单飞结果被中断: key=" + key, e);
            } catch (ExecutionException e) {
                Throwable cause = e.getCause();
                if (cause instanceof RuntimeException) {
                    throw (RuntimeException) cause;
                }
                throw new RuntimeException("单飞加载异常: key=" + key, cause);
            }
        }
    }

    /**
     * 带超时的单飞执行
     *
     * @param key          缓存键
     * @param loader       数据加载器
     * @param timeoutMs    超时时间（毫秒）
     * @param fallback     超时后的降级逻辑
     * @param <T>          返回值类型
     * @return 加载结果
     */
    @SuppressWarnings("unchecked")
    public <T> T executeWithFallback(String key, Supplier<T> loader, long timeoutMs, Supplier<T> fallback) {
        CompletableFuture<T> future = new CompletableFuture<>();
        CompletableFuture<?> existing = inflightRequests.putIfAbsent(key, future);

        if (existing == null) {
            log.debug("单飞: 获取执行权, key={}", key);
            try {
                T result = loader.get();
                future.complete(result);
                return result;
            } catch (Exception e) {
                future.completeExceptionally(e);
                throw new RuntimeException("单飞加载失败: key=" + key, e);
            } finally {
                inflightRequests.remove(key, future);
            }
        } else {
            log.debug("单飞: 等待已有请求结果, key={}, timeout={}ms", key, timeoutMs);
            try {
                return (T) existing.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("单飞等待被中断, 使用降级: key={}", key);
                return fallback.get();
            } catch (ExecutionException e) {
                log.warn("单飞加载异常, 使用降级: key={}", key);
                return fallback.get();
            }
        }
    }

    /**
     * 获取当前正在飞行中的请求数量
     */
    public int getInflightCount() {
        return inflightRequests.size();
    }

    /**
     * 检查指定 key 是否有正在进行的请求
     */
    public boolean isInflight(String key) {
        return inflightRequests.containsKey(key);
    }
}
