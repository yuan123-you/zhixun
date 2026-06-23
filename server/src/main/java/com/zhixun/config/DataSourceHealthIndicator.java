package com.zhixun.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

/**
 * 数据源健康指标
 * 监控主从数据源的可用状态和读写操作计数
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataSourceHealthIndicator implements HealthIndicator {

    private final DataSource masterDataSource;
    private final DataSource slaveDataSource;

    @Override
    public Health health() {
        Health.Builder builder = Health.up();

        // 检查主库状态
        boolean masterHealthy = DynamicDataSourceConfig.checkSlaveHealth(masterDataSource);
        builder.withDetail("masterAvailable", masterHealthy);

        // 检查从库状态
        boolean slaveHealthy = DynamicDataSourceConfig.checkSlaveHealth(slaveDataSource);
        builder.withDetail("slaveAvailable", slaveHealthy);

        // 从库运行时状态（是否被标记为不可用）
        builder.withDetail("slaveMarkedAvailable", DynamicDataSourceConfig.isSlaveAvailable());

        // 读写操作计数
        builder.withDetail("readCount", DataSourceAspect.getReadCount());
        builder.withDetail("writeCount", DataSourceAspect.getWriteCount());
        builder.withDetail("slaveFallbackCount", DataSourceAspect.getSlaveFallbackCount());

        // 如果主库不可用，标记为 DOWN
        if (!masterHealthy) {
            builder.down().withDetail("error", "主库不可用");
        } else if (!slaveHealthy) {
            // 从库不可用但主库可用，标记为 UP 但带警告
            builder.withDetail("warning", "从库不可用，读操作已回退到主库");
        }

        return builder.build();
    }
}
