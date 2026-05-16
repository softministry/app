package ro.church_office.teamleaf.desktop;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DesktopControlWindowTest {

    @Test
    void usesDevPortWhenDevProfileIsActive() {
        String previousProfiles = System.getProperty("spring.profiles.active");
        String previousPort = System.getProperty("server.port");
        String previousBaseUrl = System.getProperty("ministryadmin.desktop.base-url");
        try {
            System.clearProperty("spring.profiles.active");
            System.clearProperty("server.port");
            System.clearProperty("ministryadmin.desktop.base-url");

            assertEquals("http://localhost:85/",
                    DesktopControlWindow.resolveApplicationUrl(new String[] {"--spring.profiles.active=dev"}));
        } finally {
            restoreProperty("spring.profiles.active", previousProfiles);
            restoreProperty("server.port", previousPort);
            restoreProperty("ministryadmin.desktop.base-url", previousBaseUrl);
        }
    }

    @Test
    void usesExplicitServerPortWhenConfigured() {
        String previousPort = System.getProperty("server.port");
        try {
            System.setProperty("server.port", "9091");
            assertEquals("http://localhost:9091/", DesktopControlWindow.resolveApplicationUrl(new String[0]));
        } finally {
            restoreProperty("server.port", previousPort);
        }
    }

    @Test
    void usesExplicitBaseUrlWhenConfigured() {
        String previousBaseUrl = System.getProperty("ministryadmin.desktop.base-url");
        try {
            System.setProperty("ministryadmin.desktop.base-url", "http://localhost:9123");
            assertEquals("http://localhost:9123/", DesktopControlWindow.resolveApplicationUrl(new String[0]));
        } finally {
            restoreProperty("ministryadmin.desktop.base-url", previousBaseUrl);
        }
    }

    @Test
    void resolvesPortLabelFromApplicationUrl() {
        assertEquals("85", DesktopControlWindow.resolvePortLabel("http://localhost:85/"));
        assertEquals("8080", DesktopControlWindow.resolvePortLabel("http://localhost:8080/login"));
    }

    @Test
    void appendsDashboardPathToApplicationUrl() {
        assertEquals("http://localhost:8080/dashboard",
                DesktopControlWindow.appendPath("http://localhost:8080/", "dashboard"));
        assertEquals("http://localhost:85/dashboard",
                DesktopControlWindow.appendPath("http://localhost:85", "/dashboard"));
    }

    @Test
    void formatsStartupProgressMessageWithClampedPercent() {
        assertEquals("Pornesc aplicatia... 0%", DesktopControlWindow.startupProgressMessage(-5));
        assertEquals("Pornesc aplicatia... 42%", DesktopControlWindow.startupProgressMessage(42));
        assertEquals("Pornesc aplicatia... 100%", DesktopControlWindow.startupProgressMessage(125));
    }

    @Test
    void resolvesExplicitLogFileWhenConfigured() {
        String previousLogFile = System.getProperty("logging.file.name");
        try {
            System.setProperty("logging.file.name", "C:/tmp/ministryadmin/app.log");
            assertEquals(Path.of("C:/tmp/ministryadmin/app.log").toAbsolutePath().normalize(),
                    DesktopControlWindow.resolveLogFilePath(new String[0]));
        } finally {
            restoreProperty("logging.file.name", previousLogFile);
        }
    }

    @Test
    void resolvesLogFileFromDesktopLogsDir() {
        String previousLogFile = System.getProperty("logging.file.name");
        String previousLogsDir = System.getProperty("ministryadmin.desktop.logs-dir");
        try {
            System.clearProperty("logging.file.name");
            System.setProperty("ministryadmin.desktop.logs-dir", "C:/tmp/ministryadmin/logs");
            assertEquals(Path.of("C:/tmp/ministryadmin/logs/app.log").toAbsolutePath().normalize(),
                    DesktopControlWindow.resolveLogFilePath(new String[0]));
        } finally {
            restoreProperty("logging.file.name", previousLogFile);
            restoreProperty("ministryadmin.desktop.logs-dir", previousLogsDir);
        }
    }

    private static void restoreProperty(String key, String previousValue) {
        if (previousValue == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, previousValue);
        }
    }
}
