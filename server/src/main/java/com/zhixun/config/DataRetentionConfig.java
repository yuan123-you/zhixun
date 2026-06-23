package com.zhixun.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDateTime;

/**
 * 数据保留策略配置
 * 定期清理过期的历史数据，避免数据库膨胀
 */
@Slf4j
@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class DataRetentionConfig {

    private final JdbcTemplate jdbcTemplate;

    /** 浏览历史保留天数 */
    private static final int VIEW_HISTORY_RETENTION_DAYS = 90;

    /** 操作日志保留天数 */
    private static final int OPERATION_LOG_RETENTION_DAYS = 180;

    /** 登录日志保留天数 */
    private static final int LOGIN_LOG_RETENTION_DAYS = 180;

    /**
     * 每天凌晨 3 点清理过期的浏览历史
     * 保留最近 90 天的数据
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanUpViewHistory() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(VIEW_HISTORY_RETENTION_DAYS);
        try {
            int deleted = jdbcTemplate.update(
                    "DELETE FROM cms_view_history WHERE created_at < ?",
                    cutoff
            );
            if (deleted > 0) {
                log.info("清理浏览历史：删除 {} 条记录（{} 天以前）", deleted, VIEW_HISTORY_RETENTION_DAYS);
            }
        } catch (Exception e) {
            log.error("清理浏览历史失败", e);
        }
    }

    /**
     * 每天凌晨 3 点 30 分清理过期的操作日志
     * 保留最近 180 天的数据
     */
    @Scheduled(cron = "0 30 3 * * ?")
    public void cleanUpOperationLog() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(OPERATION_LOG_RETENTION_DAYS);
        try {
            int deleted = jdbcTemplate.update(
                    "DELETE FROM sys_operation_log WHERE created_at < ?",
                    cutoff
            );
            if (deleted > 0) {
                log.info("清理操作日志：删除 {} 条记录（{} 天以前）", deleted, OPERATION_LOG_RETENTION_DAYS);
            }
        } catch (Exception e) {
            log.error("清理操作日志失败", e);
        }
    }

    /**
     * 每天凌晨 4 点清理过期的登录日志
     * 保留最近 180 天的数据
     */
    @Scheduled(cron = "0 0 4 * * ?")
    public void cleanUpLoginLog() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(LOGIN_LOG_RETENTION_DAYS);
        try {
            int deleted = jdbcTemplate.update(
                    "DELETE FROM sys_login_log WHERE created_at < ?",
                    cutoff
            );
            if (deleted > 0) {
                log.info("清理登录日志：删除 {} 条记录（{} 天以前）", deleted, LOGIN_LOG_RETENTION_DAYS);
            }
        } catch (Exception e) {
            log.error("清理登录日志失败", e);
        }
    }

    /**
     * 每周日凌晨 5 点优化表（清理碎片空间）
     */
    @Scheduled(cron = "0 0 5 ? * SUN")
    public void optimizeTables() {
        String[] tables = {"cms_view_history", "sys_operation_log", "sys_login_log"};
        for (String table : tables) {
            try {
                jdbcTemplate.execute("OPTIMIZE TABLE " + table);
                log.info("优化表 {} 完成", table);
            } catch (Exception e) {
                log.error("优化表 {} 失败", table, e);
            }
        }
    }
}
