package ro.church_office.teamleaf.service;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ro.church_office.info.api.DatabaseService;

import javax.sql.DataSource;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.time.Instant;
import java.util.Locale;
import java.util.Properties;
import java.util.zip.ZipFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

@Service
@Primary
public class PrimaryDatabaseService extends DatabaseService {
    private final DataSource dataSource;
    private final Environment environment;

    public PrimaryDatabaseService(DataSource dataSource, Environment environment) {
        this.dataSource = dataSource;
        this.environment = environment;
    }

    @Override
    public InputStream exportDatabase(boolean fresh) throws IOException {
        String jdbcUrl = environment.getProperty("spring.datasource.url", "");
        if (jdbcUrl.toLowerCase(Locale.ROOT).startsWith("jdbc:sqlite:")) {
            return exportSqliteFile(fresh, jdbcUrl);
        }
        throw new IOException("Tip de bază de date ne-suportat pentru export: " + jdbcUrl);
    }

    @Override
    public void importDatabase(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Fișierul de import este gol.");
        }
        String jdbcUrl = environment.getProperty("spring.datasource.url", "");
        if (jdbcUrl.toLowerCase(Locale.ROOT).startsWith("jdbc:sqlite:")) {
            importSqliteFile(file, jdbcUrl);
            return;
        }
        throw new IOException("Tip de bază de date ne-suportat pentru import: " + jdbcUrl);
    }

    private InputStream exportSqliteFile(boolean fresh, String jdbcUrl) throws IOException {
        Path tmp = Files.createTempFile("db-export-", ".zip");
        try (ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(
                Files.newOutputStream(tmp, StandardOpenOption.TRUNCATE_EXISTING)))) {
            if (fresh) {
                zos.putNextEntry(new ZipEntry("README.txt"));
                String txt = "Fresh export SQLite: la import se recreează baza goală la următoarea pornire.\n";
                zos.write(txt.getBytes());
                zos.closeEntry();
            } else {
                Path sqliteFile = resolveSqlitePath(jdbcUrl);
                if (!Files.exists(sqliteFile) || !Files.isRegularFile(sqliteFile)) {
                    throw new IOException("Fișierul SQLite nu există: " + sqliteFile);
                }
                if (Files.size(sqliteFile) <= 0) {
                    throw new IOException("Fișierul SQLite este gol: " + sqliteFile);
                }
                Path snapshot = Files.createTempFile("sqlite-snapshot-", ".db");
                try {
                    createConsistentSqliteSnapshot(sqliteFile, snapshot);
                    String checksum = sha256Hex(snapshot);

                    Properties manifest = new Properties();
                    manifest.setProperty("format", "ministryadmin-backup-v1");
                    manifest.setProperty("createdAtUtc", Instant.now().toString());
                    manifest.setProperty("databaseEngine", "sqlite");
                    manifest.setProperty("databaseFile", "backup/database.sqlite");
                    manifest.setProperty("checksumFile", "backup/database.sha256");
                    manifest.setProperty("checksumSha256", checksum);

                    zos.putNextEntry(new ZipEntry("backup/manifest.properties"));
                    manifest.store(zos, "MinistryAdmin backup manifest");
                    zos.closeEntry();

                    zos.putNextEntry(new ZipEntry("backup/database.sha256"));
                    zos.write(checksum.getBytes(java.nio.charset.StandardCharsets.US_ASCII));
                    zos.write('\n');
                    zos.closeEntry();

                    zos.putNextEntry(new ZipEntry("backup/database.sqlite"));
                    Files.copy(snapshot, zos);
                    zos.closeEntry();
                } finally {
                    Files.deleteIfExists(snapshot);
                }
            }
        }
        return Files.newInputStream(tmp, StandardOpenOption.DELETE_ON_CLOSE);
    }

    private void importSqliteFile(MultipartFile file, String jdbcUrl) throws IOException {
        Path tmpZip = Files.createTempFile("db-import-", ".zip");
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, tmpZip, StandardCopyOption.REPLACE_EXISTING);
        }

        Path sqliteFile = resolveSqlitePath(jdbcUrl);
        Path parent = sqliteFile.getParent() == null ? Paths.get(".") : sqliteFile.getParent();
        Files.createDirectories(parent);

        Path extractedDb = Files.createTempFile("sqlite-import-extracted-", ".db");
        String expectedChecksum = null;
        String checksumFromFile = null;
        boolean extracted = false;
        try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(Files.newInputStream(tmpZip)))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                String name = Paths.get(entry.getName()).getFileName().toString().toLowerCase(Locale.ROOT);
                if ("readme.txt".equals(name)) {
                    continue;
                }
                if ("manifest.properties".equals(name)) {
                    Properties manifest = new Properties();
                    manifest.load(zis);
                    expectedChecksum = manifest.getProperty("checksumSha256");
                    continue;
                }
                if ("database.sha256".equals(name) || name.endsWith(".sha256")) {
                    checksumFromFile = new String(zis.readAllBytes(), java.nio.charset.StandardCharsets.US_ASCII).trim();
                    continue;
                }
                if (name.endsWith(".mv.db") || name.endsWith(".trace.db")) {
                    throw new IOException("Arhiva nu este compatibilă cu SQLite (pare backup H2).");
                }
                if (!name.endsWith(".db") && !name.endsWith(".sqlite") && !name.endsWith(".sqlite3")) {
                    continue;
                }
                try (OutputStream os = Files.newOutputStream(extractedDb, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                    zis.transferTo(os);
                }
                extracted = true;
                break;
            }
        } finally {
            Files.deleteIfExists(tmpZip);
        }

        if (!extracted) {
            throw new IOException("Arhiva pentru SQLite trebuie să conțină un fișier .db/.sqlite/.sqlite3.");
        }

        ensureSqliteFileSignature(extractedDb);
        String actualChecksum = sha256Hex(extractedDb);
        String checksumToValidate = expectedChecksum == null || expectedChecksum.isBlank() ? checksumFromFile : expectedChecksum;
        if (checksumToValidate != null && !checksumToValidate.isBlank()
                && !checksumToValidate.trim().equalsIgnoreCase(actualChecksum)) {
            throw new IOException("Checksum invalid: arhiva pare coruptă sau incompletă.");
        }
        normalizeSqliteDateColumns(extractedDb);

        safelyReplaceLiveSqliteDatabase(sqliteFile, extractedDb);
        Files.deleteIfExists(extractedDb);
    }

    private void normalizeSqliteDateColumns(Path sqliteFile) throws IOException {
        String url = "jdbc:sqlite:" + sqliteFile.toAbsolutePath();
        try (Connection connection = java.sql.DriverManager.getConnection(url);
             Statement tableStmt = connection.createStatement();
             ResultSet tables = tableStmt.executeQuery("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%'")) {
            connection.setAutoCommit(false);
            while (tables.next()) {
                String table = tables.getString(1);
                try (Statement colStmt = connection.createStatement();
                     ResultSet cols = colStmt.executeQuery("PRAGMA table_info('" + table.replace("'", "''") + "')")) {
                    while (cols.next()) {
                        String column = cols.getString("name");
                        String type = cols.getString("type");
                        if (type == null || !type.toLowerCase(Locale.ROOT).contains("date")) {
                            continue;
                        }
                        String sql = "UPDATE \"" + table + "\" SET \"" + column + "\" = \"" + column + "\" || ' 00:00:00.000' " +
                                "WHERE \"" + column + "\" GLOB '????-??-??'";
                        try (PreparedStatement ps = connection.prepareStatement(sql)) {
                            ps.executeUpdate();
                        }
                    }
                }
            }
            connection.commit();
        } catch (Exception ex) {
            throw new IOException("Normalizarea datelor calendaristice SQLite a eșuat: " + ex.getMessage(), ex);
        }
    }

    private void createConsistentSqliteSnapshot(Path sourceDb, Path snapshotDb) throws IOException {
        String url = "jdbc:sqlite:" + sourceDb.toAbsolutePath();
        String escapedSnapshot = snapshotDb.toAbsolutePath().toString().replace("'", "''");
        try (Connection connection = DriverManager.getConnection(url);
             Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA wal_checkpoint(FULL)");
            statement.execute("VACUUM INTO '" + escapedSnapshot + "'");
        } catch (SQLException ex) {
            throw new IOException("Nu am putut genera snapshot SQLite consistent pentru export: " + ex.getMessage(), ex);
        }
    }

    private void ensureSqliteFileSignature(Path sqliteFile) throws IOException {
        if (!Files.exists(sqliteFile) || Files.size(sqliteFile) < 16) {
            throw new IOException("Fișierul SQLite importat este invalid sau incomplet.");
        }
        byte[] header = new byte[16];
        try (InputStream in = Files.newInputStream(sqliteFile)) {
            int read = in.read(header);
            if (read < 16) {
                throw new IOException("Fișierul SQLite importat este prea mic.");
            }
        }
        String signature = new String(header, java.nio.charset.StandardCharsets.US_ASCII);
        if (!"SQLite format 3\u0000".equals(signature)) {
            throw new IOException("Fișierul extras din arhivă nu este un SQLite valid.");
        }
    }

    private void safelyReplaceLiveSqliteDatabase(Path liveDb, Path extractedDb) throws IOException {
        evictDataSourceConnections();
        Path backupPath = liveDb.resolveSibling(liveDb.getFileName() + ".pre-import.bak");
        if (Files.exists(liveDb) && Files.isRegularFile(liveDb)) {
            Files.copy(liveDb, backupPath, StandardCopyOption.REPLACE_EXISTING);
        }
        Files.move(extractedDb, liveDb, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        Files.deleteIfExists(liveDb.resolveSibling(liveDb.getFileName() + "-wal"));
        Files.deleteIfExists(liveDb.resolveSibling(liveDb.getFileName() + "-shm"));
        evictDataSourceConnections();
    }

    private void evictDataSourceConnections() {
        if (dataSource instanceof HikariDataSource hikari) {
            if (hikari.getHikariPoolMXBean() != null) {
                hikari.getHikariPoolMXBean().softEvictConnections();
            }
        }
    }

    private Path resolveSqlitePath(String jdbcUrl) {
        String raw = jdbcUrl.substring("jdbc:sqlite:".length());
        int paramsIdx = raw.indexOf('?');
        String filePath = paramsIdx >= 0 ? raw.substring(0, paramsIdx) : raw;
        if (filePath.startsWith("~")) {
            filePath = System.getProperty("user.home") + filePath.substring(1);
        }
        return Paths.get(filePath).toAbsolutePath().normalize();
    }

    private String sha256Hex(Path file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream in = Files.newInputStream(file)) {
                byte[] buffer = new byte[16 * 1024];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    digest.update(buffer, 0, read);
                }
            }
            byte[] hash = digest.digest();
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception ex) {
            throw new IOException("Nu am putut calcula checksum SHA-256: " + ex.getMessage(), ex);
        }
    }
}
