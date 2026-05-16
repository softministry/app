package ro.church_office.teamleaf.desktop;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("desktop")
public class DesktopEnvironmentInitializer implements ApplicationRunner {

    private final Path homeDir;
    private final Path dataDir;
    private final Path uploadsDir;
    private final Path backupsDir;
    private final Path logsDir;

    public DesktopEnvironmentInitializer(
            @Value("${ministryadmin.desktop.home-dir:${user.home}/ChurchAdministrationPlatform}") String homeDir,
            @Value("${ministryadmin.desktop.data-dir:${user.home}/ChurchAdministrationPlatform/data}") String dataDir,
            @Value("${ministryadmin.desktop.uploads-dir:${user.home}/ChurchAdministrationPlatform/uploads}") String uploadsDir,
            @Value("${ministryadmin.desktop.backups-dir:${user.home}/ChurchAdministrationPlatform/backups}") String backupsDir,
            @Value("${ministryadmin.desktop.logs-dir:${user.home}/ChurchAdministrationPlatform/logs}") String logsDir) {
        this.homeDir = Paths.get(homeDir).toAbsolutePath().normalize();
        this.dataDir = Paths.get(dataDir).toAbsolutePath().normalize();
        this.uploadsDir = Paths.get(uploadsDir).toAbsolutePath().normalize();
        this.backupsDir = Paths.get(backupsDir).toAbsolutePath().normalize();
        this.logsDir = Paths.get(logsDir).toAbsolutePath().normalize();
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        createDir(homeDir);
        createDir(dataDir);
        createDir(uploadsDir);
        createDir(backupsDir);
        createDir(logsDir);
        createDir(uploadsDir.resolve("church"));
    }

    private void createDir(Path dir) throws IOException {
        Files.createDirectories(dir);
    }
}
