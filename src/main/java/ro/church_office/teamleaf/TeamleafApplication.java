package ro.church_office.teamleaf;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

import ro.church_office.teamleaf.desktop.DesktopControlWindow;

@SpringBootApplication(scanBasePackages = "ro.church_office")
@EnableJpaRepositories(basePackages = "ro.church_office")
@EntityScan(basePackages = "ro.church_office")
@EnableScheduling
public class TeamleafApplication {
    public static void main(String[] args) {
        configureEmbeddedDatabaseDefaults();

        if (shouldUseDesktopControlWindow()) {
            DesktopControlWindow.show(args);
            return;
        }

        SpringApplication.run(TeamleafApplication.class, args);
    }

    private static void configureEmbeddedDatabaseDefaults() {
        Path dataDir = Path.of(System.getProperty("user.home"), "ChurchAdministrationPlatform", "data")
                .toAbsolutePath()
                .normalize();
        try {
            Files.createDirectories(dataDir);
        } catch (Exception ignored) {
            // Spring/SQLite will report a clear startup error if the directory cannot be created.
        }

        Path databaseFile = dataDir.resolve("ministryadmin.sqlite.db");

        setDefaultProperty("spring.datasource.url", "SPRING_DATASOURCE_URL", "jdbc:sqlite:" + databaseFile);
        setDefaultProperty("spring.datasource.driver-class-name", "SPRING_DATASOURCE_DRIVER_CLASS_NAME", "org.sqlite.JDBC");
        setDefaultProperty("spring.jpa.database-platform", "SPRING_JPA_DATABASE_PLATFORM", "org.hibernate.community.dialect.SQLiteDialect");
        setDefaultProperty("spring.jpa.hibernate.ddl-auto", "SPRING_JPA_HIBERNATE_DDL_AUTO", "update");
        setDefaultProperty("spring.flyway.enabled", "SPRING_FLYWAY_ENABLED", "false");
    }

    private static void setDefaultProperty(String propertyName, String environmentName, String value) {
        if (System.getProperty(propertyName) == null && System.getenv(environmentName) == null) {
            System.setProperty(propertyName, value);
        }
    }

    private static boolean shouldUseDesktopControlWindow() {
        if (Boolean.getBoolean("ministryadmin.cli")) {
            return false;
        }
        if ("false".equalsIgnoreCase(System.getProperty("ministryadmin.desktop.control-window.enabled"))) {
            return false;
        }
        if (java.awt.GraphicsEnvironment.isHeadless()) {
            return false;
        }
        String osName = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        return osName.contains("win") || osName.contains("mac");
    }
}
