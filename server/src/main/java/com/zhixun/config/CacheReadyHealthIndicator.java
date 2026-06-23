package com.zhixun.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CacheReadyHealthIndicator implements HealthIndicator {

    @Override
    public Health health() {
        if (CacheWarmUpRunner.isCacheReady()) {
            return Health.up().withDetail("cacheWarmUp", "ready").build();
        }
        return Health.outOfService().withDetail("cacheWarmUp", "not ready").build();
    }
}
