package com.scaffold.system.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 为已存在的甄选结果表补充后续版本新增字段。
 * 新安装仍由 selection_module.sql 创建完整表结构。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@ConditionalOnProperty(name = "scaffold.migrations.enabled", havingValue = "true", matchIfMissing = true)
public class SelectionSchemaMigration implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(SelectionSchemaMigration.class);
    private final JdbcTemplate jdbc;

    public SelectionSchemaMigration(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!tableExists("biz_selection_project")) return;
        Map<String, String> columns = new LinkedHashMap<>();
        columns.put("publicity_website",
                "VARCHAR(500) DEFAULT 'https://xe.sd.chinamobile.com/pms-portal-react/#/console4'");
        columns.put("review_report_name", "VARCHAR(255) DEFAULT NULL");
        columns.put("review_report_path", "VARCHAR(500) DEFAULT NULL");
        columns.forEach(this::addColumnIfMissing);
    }

    private boolean tableExists(String table) {
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(*) FROM information_schema.tables
                WHERE table_schema = DATABASE() AND table_name = ?
                """, Integer.class, table);
        return count != null && count > 0;
    }

    private void addColumnIfMissing(String column, String definition) {
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(*) FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = 'biz_selection_project'
                  AND column_name = ?
                """, Integer.class, column);
        if (count != null && count > 0) return;
        jdbc.execute("ALTER TABLE biz_selection_project ADD COLUMN " + column + " " + definition);
        log.info("甄选结果数据库升级：已新增字段 biz_selection_project.{}", column);
    }
}
