package com.zhixun.service.impl;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import static org.junit.jupiter.api.Assertions.*;

class ArticleReviewSchemaTest {
    @Test void legacyRowsReceiveUnknownPermissionNotFabricatedApproval() {
        var source = new DriverManagerDataSource("jdbc:h2:mem:review_migration_" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        var jdbc = new JdbcTemplate(source);
        try {
            jdbc.execute("create table cms_article(id bigint primary key,status int,publish_at timestamp)");
            jdbc.update("insert into cms_article values(1,1,current_timestamp),(2,2,current_timestamp),(3,1,null)");
            new ResourceDatabasePopulator(new FileSystemResource("../db/migrations/2026-10-08-article-review-state.sql")).execute(source);
            assertEquals(3, jdbc.queryForObject("select count(*) from cms_article where review_state='UNVERIFIED'", Integer.class));
            assertEquals(2, jdbc.queryForObject("select status from cms_article where id=2", Integer.class));
            assertEquals(0, jdbc.queryForObject("select count(*) from cms_article where review_state in ('CLEARED','APPROVED')", Integer.class));
        } finally { jdbc.execute("drop all objects"); }
    }
    @Test void initialSchemasAndStandaloneMigrationAgreeOnFailClosedDefault() throws Exception {
        String schema = Files.readString(Path.of("src/main/resources/schema.sql"));
        String originalInit = Files.readString(Path.of("src/main/resources/db/01-schema.sql"));
        String migration = Files.readString(Path.of("../db/migrations/2026-10-08-article-review-state.sql"));
        for (String sql : new String[]{schema, originalInit, migration}) {
            assertTrue(sql.contains("review_state VARCHAR(16) NOT NULL DEFAULT 'UNVERIFIED'"));
        }
        assertFalse(migration.toUpperCase().contains("UPDATE CMS_ARTICLE"));
    }
}
