package com.zhixun.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * Redis 配置
 * 仅在 spring.data.redis.enabled=true（默认）时激活
 */
@Configuration
@ConditionalOnProperty(name = "spring.data.redis.enabled", havingValue = "true", matchIfMissing = true)
public class RedisConfig {

    /** TTL 抖动比例（默认 ±15%） */
    private static final double TTL_JITTER_RATIO = 0.15;

    /** 最小 TTL（秒） */
    private static final long MIN_TTL_SECONDS = 1;

    /** 数据版本号 Redis Key */
    public static final String DATA_VERSION_KEY = "data:version";

    /**
     * 递增数据版本号
     */
    public static long incrementDataVersion(StringRedisTemplate stringRedisTemplate) {
        return stringRedisTemplate.opsForValue().increment(DATA_VERSION_KEY);
    }

    /**
     * 获取当前数据版本号
     */
    public static long getDataVersion(StringRedisTemplate stringRedisTemplate) {
        String version = stringRedisTemplate.opsForValue().get(DATA_VERSION_KEY);
        return version != null ? Long.parseLong(version) : 0;
    }

    @Bean

    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // 自定义 ObjectMapper，支持 Java 8 时间类型
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        objectMapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.NON_FINAL);
        objectMapper.registerModule(new JavaTimeModule());

        // Key 使用 String 序列化
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);

        // Value 使用 JSON 序列化
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(objectMapper);
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        return template;
    }
@Bean

    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }

    @Bean

    public RedisMessageListenerContainer redisMessageListenerContainer(RedisConnectionFactory connectionFactory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        return container;
    }

    /**
     * 为 TTL 添加随机抖动，防止大量缓存同时失效导致雪崩
     * 抖动范围为 ±15%
     *
     * @param baseTTLSeconds 基础 TTL（秒）
     * @return 添加随机抖动后的 TTL（秒）
     */
    public static long jitteredTTL(long baseTTLSeconds) {
        if (baseTTLSeconds <= 0) {
            return baseTTLSeconds;
        }
        long offset = Math.max(1, (long) (baseTTLSeconds * TTL_JITTER_RATIO));
        long randomOffset = ThreadLocalRandom.current().nextLong(-offset, offset + 1);
        long finalTTL = baseTTLSeconds + randomOffset;
        return Math.max(MIN_TTL_SECONDS, finalTTL);
    }

    /**
     * 带抖动的 TTL 转换：将分钟转为秒并添加抖动
     *
     * @param baseTTLMinutes 基础 TTL（分钟）
     * @return 添加随机抖动后的 TTL（秒）
     */
    public static long jitteredTTLFromMinutes(long baseTTLMinutes) {
        return jitteredTTL(TimeUnit.MINUTES.toSeconds(baseTTLMinutes));
    }

    /**
     * 带抖动的 TTL 转换：将小时转为秒并添加抖动
     *
     * @param baseTTLHours 基础 TTL（小时）
     * @return 添加随机抖动后的 TTL（秒）
     */
    public static long jitteredTTLFromHours(long baseTTLHours) {
        return jitteredTTL(TimeUnit.HOURS.toSeconds(baseTTLHours));
    }
}
