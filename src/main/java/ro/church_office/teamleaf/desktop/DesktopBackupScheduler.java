package ro.church_office.teamleaf.desktop;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Profile("desktop")
public class DesktopBackupScheduler {

    private static final DateTimeFormatter FILE_TS = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final boolean backupEnabled;
    private final Path backupsDir;
    private final Path databaseFileBase;
    private final int retentionDays;

    public DesktopBackupScheduler(
            @Value("${ministryadmin.desktop.backup.enabled:true}") boolean backupEnabled,
            @Value("${ministryadmin.desktop.backups-dir:${user.home}/ChurchAdministrationPlatform/backups}") String backupsDir,
            @Value("${ministryadmin.desktop.database-file-base:${user.home}/ChurchAdministrationPlatform/data/ministryadmin-db}") String databaseFileBase,
            @Value("${ministryadmin.desktop.backup.retention-days:30}") int retentionDays) {
        this.backupEnabled = backupEnabled;
        this.backupsDir = Paths.get(backupsDir).toAbsolutePath().normalize();
        this.databaseFileBase = Paths.get(databaseFileBase).toAbsolutePath().normalize();
        this.retentionDays = Math.max(1, retentionDays);
    }

    @Scheduled(
            initialDelayString = "${ministryadmin.desktop.backup.initial-delay-ms:120000}",
            fixedDelayString = "${ministryadmin.desktop.backup.interval-ms:86400000}")
    public void runBackup() {
        if (!backupEnabled) {
            return;
        }
        try {
            Files.createDirectories(backupsDir);
            backupIfExists(databaseFileBase.resolveSibling(databaseFileBase.getFileName() + ".mv.db"), "mv");
            backupIfExists(databaseFileBase.resolveSibling(databaseFileBase.getFileName() + ".trace.db"), "trace");
            cleanupOldBackups();
        } catch (Exception ignored) {
            // Backup must never block startup/runtime.
        }
    }

    private void backupIfExists(Path source, String suffix) throws IOException {
        if (!Files.exists(source) || !Files.isRegularFile(source)) {
            return;
        }
        String ts = LocalDateTime.now().format(FILE_TS);
        Path destination = backupsDir.resolve("ministryadmin-" + suffix + "-" + ts + ".db");
        Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
    }

    private void cleanupOldBackups() throws IOException {
        LocalDateTime cutoff = LocalDateTime.now().minus(retentionDays, ChronoUnit.DAYS);
        try (var stream = Files.list(backupsDir)) {
            stream.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().startsWith("ministryadmin-"))
                    .sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            LocalDateTime modifiedAt = LocalDateTime.ofInstant(
                                    Files.getLastModifiedTime(path).toInstant(),
                                    java.time.ZoneId.systemDefault());
                            if (modifiedAt.isBefore(cutoff)) {
                                Files.deleteIfExists(path);
                            }
                        } catch (IOException ignored) {
                            // Best effort cleanup.
                        }
                    });
        }
    }
}
