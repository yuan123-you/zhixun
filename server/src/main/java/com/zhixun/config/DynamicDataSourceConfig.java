package com.zhixun.config;

import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 动态数据源配置
 * 实现主从读写分离，基于 AbstractRoutingDataSource 路由数据源
 * 支持从库健康检测和自动回退到主库
 */
@Slf4j
@Configuration
public class DynamicDataSourceConfig {

    /** 从库是否可用（用于故障回退） */
    private static final AtomicBoolean SLAVE_AVAILABLE = new AtomicBoolean(true);

    /** 上次从库检测时间 */
    private static volatile long LAST_CHECK_TIME = 0;

    /** 检测间隔：30秒 */
    private static final long CHECK_INTERVAL_MS = 30000L;

    /**
     * 获取从库是否可用状态
     */
    public static boolean isSlaveAvailable() {
        return SLAVE_AVAILABLE.get();
    }

    /**
     * 标记从库不可用（由 DataSourceAspect 在异常时调用）
     */
    public static void markSlaveUnavailable() {
        if (SLAVE_AVAILABLE.compareAndSet(true, false)) {
            log.warn("从库已标记为不可用，后续读操作将回退到主库");
        }
    }

    /**
     * 尝试恢复从库可用状态
     */
    public static void tryRecoverSlave() {
        long now = System.currentTimeMillis();
        // 检测间隔内不重复检测
        if (now - LAST_CHECK_TIME < CHECK_INTERVAL_MS) {
            return;
        }
        LAST_CHECK_TIME = now;
        SLAVE_AVAILABLE.set(true);
        log.info("尝试恢复从库可用状态");
    }

    /**
     * 主库数据源配置
     */
    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.master")
    public DataSource masterDataSource() {
        log.info("初始化主库数据源");
        return new HikariDataSource();
    }

    /**
     * 从库数据源配置
     */
    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.slave")
    public DataSource slaveDataSource() {
        log.info("初始化从库数据源");
        return new HikariDataSource();
    }

    /**
     * 动态数据源路由
     * 根据 DataSourceContextHolder 中的数据源类型选择主库或从库
     */
    @Bean
    @Primary
    public DataSource dynamicDataSource(DataSource masterDataSource, DataSource slaveDataSource) {
        Map<Object, Object> targetDataSources = new HashMap<>(2);
        targetDataSources.put(DataSourceContextHolder.MASTER, masterDataSource);
        targetDataSources.put(DataSourceContextHolder.SLAVE, slaveDataSource);

        AbstractRoutingDataSource routingDataSource = new AbstractRoutingDataSource() {
            @Override
            protected Object determineCurrentLookupKey() {
                String dataSourceType = DataSourceContextHolder.getDataSourceType();
                log.debug("当前数据源: {}", dataSourceType);
                return dataSourceType;
            }
        };

        routingDataSource.setDefaultTargetDataSource(masterDataSource);
        routingDataSource.setTargetDataSources(targetDataSources);
        routingDataSource.afterPropertiesSet();

        log.info("动态数据源配置完成 - 主从读写分离已启用");
        return routingDataSource;
    }

    /**
     * 检查从库连接是否正常
     *
     * @param slaveDataSource 从库数据源
     * @return true-连接正常，false-连接异常
     */
    public static boolean checkSlaveHealth(DataSource slaveDataSource) {
        try (Connection conn = slaveDataSource.getConnection()) {
            return conn.isValid(3); // 3秒超时
        } catch (SQLException e) {
            log.warn("从库健康检查失败: {}", e.getMessage());
            return false;
        }
    }
}
