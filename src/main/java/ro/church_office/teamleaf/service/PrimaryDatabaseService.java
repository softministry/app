package ro.church_office.teamleaf.service;

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
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.util.Locale;
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
                zos.putNextEntry(new ZipEntry(sqliteFile.getFileName().toString()));
                Files.copy(sqliteFile, zos);
                zos.closeEntry();
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
                if (name.endsWith(".mv.db") || name.endsWith(".trace.db")) {
                    throw new IOException("Arhiva nu este compatibilă cu SQLite (pare backup H2).");
                }
                if (!name.endsWith(".db") && !name.endsWith(".sqlite") && !name.endsWith(".sqlite3")) {
                    continue;
                }
                try (OutputStream os = Files.newOutputStream(sqliteFile, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
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

        normalizeSqliteDateColumns(sqliteFile);
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

    private Path resolveSqlitePath(String jdbcUrl) {
        String raw = jdbcUrl.substring("jdbc:sqlite:".length());
        int paramsIdx = raw.indexOf('?');
        String filePath = paramsIdx >= 0 ? raw.substring(0, paramsIdx) : raw;
        if (filePath.startsWith("~")) {
            filePath = System.getProperty("user.home") + filePath.substring(1);
        }
        return Paths.get(filePath).toAbsolutePath().normalize();
    }
}
