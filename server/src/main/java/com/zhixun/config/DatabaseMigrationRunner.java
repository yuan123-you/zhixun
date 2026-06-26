package com.zhixun.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 数据库迁移执行器 —— 启动时自动执行 db/migration/ 下的 SQL 文件。
 * 使用 flyway 命名约定：V{version}__{description}.sql
 * 通过 migration_history 表追踪已执行的迁移。
 */
@Slf4j
@Component
public class DatabaseMigrationRunner implements ApplicationRunner {

    private final DataSource masterDataSource;

    private static final Pattern VERSION_PATTERN = Pattern.compile("^V(\\d+)__.*\\.sql$");

    public DatabaseMigrationRunner(@Qualifier("masterDataSource") DataSource masterDataSource) {
        this.masterDataSource = masterDataSource;
    }

    @Override
    public void run(ApplicationArguments args) {
        try (Connection conn = masterDataSource.getConnection()) {
            ensureHistoryTable(conn);
            int latestVersion = getLatestVersion(conn);
            log.info("Database migration: current latest version = {}", latestVersion);

            // Scan and execute pending migrations
            String[] files = listMigrationFiles();
            for (String filename : files) {
                Matcher m = VERSION_PATTERN.matcher(filename);
                if (!m.matches()) continue;

                int version = Integer.parseInt(m.group(1));
                if (version > latestVersion) {
                    log.info("Executing migration: {}", filename);
                    Resource resource = new ClassPathResource("db/migration/" + filename);
                    ScriptUtils.executeSqlScript(conn, resource);
                    recordMigration(conn, version, filename);
                    log.info("Migration {} completed successfully", filename);
                } else {
                    log.debug("Skipping already applied migration: {}", filename);
                }
            }
            log.info("Database migration check complete");
        } catch (Exception e) {
            log.error("Database migration failed", e);
            // Don't fail startup — allow app to start even if migration has issues
        }
    }

    private void ensureHistoryTable(Connection conn) throws Exception {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS migration_history (" +
                "  id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                "  version INT NOT NULL UNIQUE," +
                "  description VARCHAR(200) NOT NULL," +
                "  executed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
            );
        }
    }

    private int getLatestVersion(Connection conn) throws Exception {
        try (Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT COALESCE(MAX(version), 0) FROM migration_history");
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private void recordMigration(Connection conn, int version, String description) throws Exception {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(String.format(
                "INSERT INTO migration_history (version, description) VALUES (%d, '%s')",
                version, description.replace("'", "''")
            ));
        }
    }

    /**
     * List migration SQL files in sorted order.
     * Spring Boot ClassPathResource doesn't support directory listing directly,
     * so we maintain an explicit list of known migrations.
     */
    private String[] listMigrationFiles() {
        return new String[] {
            "V2__new_features.sql"
            // Add future migrations here: "V3__xxx.sql", "V4__xxx.sql", etc.
        };
    }
}
