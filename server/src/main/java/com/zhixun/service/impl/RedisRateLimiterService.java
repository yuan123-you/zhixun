package com.zhixun.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Redis 分布式令牌桶限流服务
 * 使用 Redis + Lua 脚本实现分布式环境下的精确限流
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisRateLimiterService {

    private final StringRedisTemplate stringRedisTemplate;

    /** 缓存不同限流规则的 Lua 脚本 */
    private final ConcurrentHashMap<String, DefaultRedisScript<List>> scriptCache = new ConcurrentHashMap<>();

    /** 限流 Key 前缀 */
    private static final String RATE_LIMIT_KEY_PREFIX = "rate_limit:";

    /** Lua 脚本 */
    private static final String LUA_SCRIPT_PATH = "scripts/rate_limiter.lua";

    /**
     * 尝试获取令牌（使用默认规则：每秒10个令牌，最大容量10）
     *
     * @param resourceId 资源标识（如接口路径、用户ID等）
     * @return 限流结果
     */
    public RateLimitResult tryAcquire(String resourceId) {
        return tryAcquire(resourceId, 10, 10, 1);
    }

    /**
     * 尝试获取令牌
     *
     * @param resourceId      资源标识
     * @param maxTokens       令牌桶最大容量
     * @param tokensPerSecond 令牌生成速率（每秒）
     * @param requestedTokens 本次请求消耗的令牌数
     * @return 限流结果
     */
    public RateLimitResult tryAcquire(String resourceId, int maxTokens, int tokensPerSecond, int requestedTokens) {
        String key = RATE_LIMIT_KEY_PREFIX + resourceId;
        long now = System.currentTimeMillis();

        try {
            DefaultRedisScript<List> script = getScript();
            List<Long> result = stringRedisTemplate.execute(
                    script,
                    Collections.singletonList(key),
                    String.valueOf(maxTokens),
                    String.valueOf(tokensPerSecond),
                    String.valueOf(requestedTokens),
                    String.valueOf(now)
            );

            if (result != null && result.size() >= 3) {
                boolean allowed = result.get(0) != null && result.get(0) == 1L;
                long remainingTokens = result.get(1) != null ? result.get(1) : 0L;
                long waitTime = result.get(2) != null ? result.get(2) : 0L;

                if (!allowed) {
                    log.warn("限流触发: resourceId={}, remainingTokens={}, waitTime={}ms",
                            resourceId, remainingTokens, waitTime);
                }

                return new RateLimitResult(allowed, remainingTokens, waitTime);
            }
        } catch (Exception e) {
            log.error("Redis 限流执行异常，降级为放行: resourceId={}, error={}", resourceId, e.getMessage());
            // Redis 异常时降级为放行，避免影响正常业务
            return new RateLimitResult(true, maxTokens, 0);
        }

        // 脚本执行异常时放行
        return new RateLimitResult(true, maxTokens, 0);
    }

    /**
     * 检查用户级别限流
     *
     * @param userId    用户ID
     * @param action    操作类型（如 login、comment、search 等）
     * @param maxTokens 最大令牌数
     * @param rate      每秒生成令牌数
     * @return 是否允许
     */
    public boolean isAllowedByUser(Long userId, String action, int maxTokens, int rate) {
        String resourceId = "user:" + userId + ":" + action;
        RateLimitResult result = tryAcquire(resourceId, maxTokens, rate, 1);
        return result.isAllowed();
    }

    /**
     * 检查IP级别限流
     *
     * @param ip        客户端IP
     * @param action    操作类型
     * @param maxTokens 最大令牌数
     * @param rate      每秒生成令牌数
     * @return 是否允许
     */
    public boolean isAllowedByIp(String ip, String action, int maxTokens, int rate) {
        String resourceId = "ip:" + ip + ":" + action;
        RateLimitResult result = tryAcquire(resourceId, maxTokens, rate, 1);
        return result.isAllowed();
    }

    /**
     * 检查全局接口限流
     *
     * @param apiPath   接口路径
     * @param maxTokens 最大令牌数
     * @param rate      每秒生成令牌数
     * @return 是否允许
     */
    public boolean isAllowedGlobally(String apiPath, int maxTokens, int rate) {
        String resourceId = "global:" + apiPath;
        RateLimitResult result = tryAcquire(resourceId, maxTokens, rate, 1);
        return result.isAllowed();
    }

    /**
     * 获取 Lua 脚本实例
     */
    @SuppressWarnings("unchecked")
    private DefaultRedisScript<List> getScript() {
        return scriptCache.computeIfAbsent("rate_limiter", k -> {
            DefaultRedisScript<List> script = new DefaultRedisScript<>();
            script.setLocation(new ClassPathResource(LUA_SCRIPT_PATH));
            script.setResultType(List.class);
            return script;
        });
    }

    /**
     * 限流结果
     */
    public static class RateLimitResult {
        private final boolean allowed;
        private final long remainingTokens;
        private final long waitTimeMs;

        public RateLimitResult(boolean allowed, long remainingTokens, long waitTimeMs) {
            this.allowed = allowed;
            this.remainingTokens = remainingTokens;
            this.waitTimeMs = waitTimeMs;
        }

        public boolean isAllowed() {
            return allowed;
        }

        public long getRemainingTokens() {
            return remainingTokens;
        }

        public long getWaitTimeMs() {
            return waitTimeMs;
        }
    }
}
