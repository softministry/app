package ro.church_office.teamleaf.integration;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

abstract class AbstractContainerIT {

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        String sqlitePath = System.getProperty("java.io.tmpdir") + "/ministryadmin-it.sqlite.db";
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:" + sqlitePath);
        registry.add("spring.datasource.driver-class-name", () -> "org.sqlite.JDBC");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.community.dialect.SQLiteDialect");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "update");
        registry.add("spring.flyway.enabled", () -> "false");
        registry.add("ministryadmin.initial-admin-password", () -> "IntegrationTest123!");
    }
}
