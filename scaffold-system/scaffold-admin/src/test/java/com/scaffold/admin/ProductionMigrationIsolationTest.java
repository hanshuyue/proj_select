package com.scaffold.admin;

import com.scaffold.system.service.CollaborationSchemaMigration;
import com.scaffold.system.service.SelectionSchemaMigration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ProductionMigrationIsolationTest {
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
        .withBean(JdbcTemplate.class, () -> jdbc)
        .withUserConfiguration(CollaborationSchemaMigration.class, SelectionSchemaMigration.class);

    @Test void productionDisablesAutomaticBusinessAndSchemaMutations() {
        runner.withPropertyValues("scaffold.migrations.enabled=false").run(context -> {
            assertThat(context).doesNotHaveBean(CollaborationSchemaMigration.class);
            assertThat(context).doesNotHaveBean(SelectionSchemaMigration.class);
        });
    }
    @Test void developmentRetainsMigrationBeans() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(CollaborationSchemaMigration.class);
            assertThat(context).hasSingleBean(SelectionSchemaMigration.class);
        });
    }
}
