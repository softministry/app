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
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

@Service
@Primary
public class PrimaryDatabaseService extends DatabaseService {
    private static final long PROCESS_TIMEOUT_SECONDS = 120L;
    private final DataSource dataSource;
    private final Environment environment;

    public PrimaryDatabaseService(DataSource dataSource, Environment environment) {
        this.dataSource = dataSource;
        this.environment = environment;
    }

    @Override
    public InputStream exportDatabase(boolean fresh) throws IOException {
        String jdbcUrl = environment.getProperty("spring.datasource.url", "");
        if (jdbcUrl.toLowerCase(Locale.ROOT).startsWith("jdbc:postgresql:")) {
            return exportPostgres(fresh, jdbcUrl);
        }
        if (jdbcUrl.toLowerCase(Locale.ROOT).startsWith("jdbc:h2:file:")) {
            return exportH2File(fresh, jdbcUrl);
        }
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
        if (jdbcUrl.toLowerCase(Locale.ROOT).startsWith("jdbc:postgresql:")) {
            importPostgres(file, jdbcUrl);
            return;
        }
        if (jdbcUrl.toLowerCase(Locale.ROOT).startsWith("jdbc:h2:file:")) {
            importH2File(file, jdbcUrl);
            return;
        }
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
            // fresh import
            Files.deleteIfExists(sqliteFile);
            return;
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

    private InputStream exportH2File(boolean fresh, String jdbcUrl) throws IOException {
        Path tmp = Files.createTempFile("db-export-", ".zip");
        try (ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(
                Files.newOutputStream(tmp, StandardOpenOption.TRUNCATE_EXISTING)))) {
            if (fresh) {
                zos.putNextEntry(new ZipEntry("README.txt"));
                String txt = "Fresh export: baza va fi recreată la import/rulare.\n";
                zos.write(txt.getBytes());
                zos.closeEntry();
            } else {
                Path base = resolveH2BasePath(jdbcUrl);
                List<Path> candidates = listH2Files(base);
                if (candidates.isEmpty()) {
                    throw new IOException("Nu am găsit fișiere H2 pentru export la: " + base);
                }
                for (Path p : candidates) {
                    zos.putNextEntry(new ZipEntry(p.getFileName().toString()));
                    Files.copy(p, zos);
                    zos.closeEntry();
                }
            }
        }
        return Files.newInputStream(tmp, StandardOpenOption.DELETE_ON_CLOSE);
    }

    private void importH2File(MultipartFile file, String jdbcUrl) throws IOException {
        Path tmp = Files.createTempFile("db-import-", ".zip");
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
        }

        boolean hasDbFiles = false;
        try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(Files.newInputStream(tmp)))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (!entry.isDirectory() && !entry.getName().equalsIgnoreCase("README.txt")) {
                    hasDbFiles = true;
                    break;
                }
            }
        }

        Path base = resolveH2BasePath(jdbcUrl);
        Path parent = base.getParent() == null ? Paths.get(".") : base.getParent();
        Files.createDirectories(parent);

        if (!hasDbFiles) {
            for (Path p : listH2Files(base)) {
                Files.deleteIfExists(p);
            }
            return;
        }

        try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(Files.newInputStream(tmp)))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                String name = Paths.get(entry.getName()).getFileName().toString();
                if ("README.txt".equalsIgnoreCase(name)) {
                    continue;
                }
                Path out = parent.resolve(name).normalize();
                if (!out.startsWith(parent.normalize())) {
                    throw new IOException("Intrare ZIP invalidă: " + entry.getName());
                }
                try (OutputStream os = new BufferedOutputStream(
                        Files.newOutputStream(out, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))) {
                    zis.transferTo(os);
                }
            }
        }
    }

    private InputStream exportPostgres(boolean fresh, String jdbcUrl) throws IOException {
        String dbName = extractPostgresDbName(jdbcUrl);
        String host = environment.getProperty("DB_HOST", "localhost");
        String port = environment.getProperty("DB_PORT", "5432");
        String user = environment.getProperty("DB_USER",
                environment.getProperty("spring.datasource.username", "postgres"));
        String pass = environment.getProperty("DB_PASSWORD",
                environment.getProperty("spring.datasource.password", ""));

        Path tmp = Files.createTempFile("db-export-", ".zip");
        Path sqlTmp = Files.createTempFile("db-export-", ".sql");
        List<String> cmd = new ArrayList<>();
        cmd.add("pg_dump");
        cmd.add("-h");
        cmd.add(host);
        cmd.add("-p");
        cmd.add(port);
        cmd.add("-U");
        cmd.add(user);
        if (fresh) {
            cmd.add("--schema-only");
        }
        cmd.add("--no-owner");
        cmd.add("--no-privileges");
        cmd.add("-f");
        cmd.add(sqlTmp.toString());
        cmd.add(dbName);

        boolean dumped = runLocalPgDump(cmd, pass);
        if (!dumped) {
            runDockerPgDump(host, port, user, pass, dbName, fresh, sqlTmp);
        }

        try (ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(
                Files.newOutputStream(tmp, StandardOpenOption.TRUNCATE_EXISTING)))) {
            zos.putNextEntry(new ZipEntry("backup.sql"));
            Files.copy(sqlTmp, zos);
            zos.closeEntry();
        } finally {
            Files.deleteIfExists(sqlTmp);
        }
        return Files.newInputStream(tmp, StandardOpenOption.DELETE_ON_CLOSE);
    }

    private void importPostgres(MultipartFile file, String jdbcUrl) throws IOException {
        String dbName = extractPostgresDbName(jdbcUrl);
        String host = environment.getProperty("DB_HOST", "localhost");
        String port = environment.getProperty("DB_PORT", "5432");
        String user = environment.getProperty("DB_USER",
                environment.getProperty("spring.datasource.username", "postgres"));
        String pass = environment.getProperty("DB_PASSWORD",
                environment.getProperty("spring.datasource.password", ""));

        Path tmpZip = Files.createTempFile("db-import-", ".zip");
        Path sqlTmp = Files.createTempFile("db-import-", ".sql");
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, tmpZip, StandardCopyOption.REPLACE_EXISTING);
        }

        boolean extracted = false;
        try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(Files.newInputStream(tmpZip)))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                String lower = entry.getName().toLowerCase(Locale.ROOT);
                if (lower.endsWith(".sql")) {
                    try (OutputStream os = Files.newOutputStream(sqlTmp, StandardOpenOption.TRUNCATE_EXISTING)) {
                        zis.transferTo(os);
                    }
                    extracted = true;
                    break;
                }
            }
        }
        if (!extracted) {
            throw new IOException("Arhiva pentru PostgreSQL trebuie să conțină un fișier .sql.");
        }

        List<String> cmd = List.of(
                "psql", "-h", host, "-p", port, "-U", user, "-d", dbName, "-f", sqlTmp.toString());
        boolean imported = runLocalPsql(cmd, pass);
        if (!imported) {
            runDockerPsql(host, port, user, pass, dbName, sqlTmp);
        }
        try {
            Files.deleteIfExists(tmpZip);
            Files.deleteIfExists(sqlTmp);
        } finally {
            Files.deleteIfExists(tmpZip);
            Files.deleteIfExists(sqlTmp);
        }
    }

    private boolean runLocalPgDump(List<String> command, String pass) throws IOException {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.environment().put("PGPASSWORD", pass == null ? "" : pass);
        pb.redirectErrorStream(true);
        Process p;
        try {
            p = pb.start();
        } catch (IOException ex) {
            return false;
        }
        byte[] mergedOutput = p.getInputStream().readAllBytes();
        try {
            int code = waitForProcess(p, "pg_dump");
            if (code != 0) {
                String err = new String(mergedOutput);
                throw new IOException("pg_dump a eșuat: " + err.trim());
            }
            return true;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IOException("Export PostgreSQL întrerupt.", ex);
        }
    }

    private boolean runLocalPsql(List<String> command, String pass) throws IOException {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.environment().put("PGPASSWORD", pass == null ? "" : pass);
        pb.redirectErrorStream(true);
        Process p;
        try {
            p = pb.start();
        } catch (IOException ex) {
            return false;
        }
        byte[] mergedOutput = p.getInputStream().readAllBytes();
        try {
            int code = waitForProcess(p, "psql");
            if (code != 0) {
                String err = new String(mergedOutput);
                throw new IOException("psql import a eșuat: " + err.trim());
            }
            return true;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IOException("Import PostgreSQL întrerupt.", ex);
        }
    }

    private void runDockerPgDump(String host, String port, String user, String pass, String dbName, boolean fresh, Path outputSql) throws IOException {
        String container = resolveDockerContainerName();
        if (container == null || container.isBlank()) {
            throw new IOException("Nu găsesc 'pg_dump' local și nici numele containerului Docker. Setează DB_DOCKER_CONTAINER.");
        }

        List<String> cmd = new ArrayList<>(Arrays.asList(
                "docker", "exec", "-e", "PGPASSWORD=" + (pass == null ? "" : pass), container,
                "pg_dump", "-h", host, "-p", port, "-U", user, "--no-owner", "--no-privileges"));
        if (fresh) {
            cmd.add("--schema-only");
        }
        cmd.add(dbName);

        ProcessBuilder pb = new ProcessBuilder(cmd);
        Process p = pb.start();
        java.io.ByteArrayOutputStream stderrBuffer = new java.io.ByteArrayOutputStream();
        Thread stderrDrainer = new Thread(() -> {
            try (InputStream err = p.getErrorStream()) {
                err.transferTo(stderrBuffer);
            } catch (IOException ignored) {
            }
        }, "pg-docker-dump-stderr");
        stderrDrainer.setDaemon(true);
        stderrDrainer.start();
        try (InputStream in = p.getInputStream();
             OutputStream out = Files.newOutputStream(outputSql, StandardOpenOption.TRUNCATE_EXISTING)) {
            in.transferTo(out);
        }
        try {
            int code = waitForProcess(p, "docker exec pg_dump");
            if (code != 0) {
                String err = stderrBuffer.toString();
                throw new IOException("docker exec pg_dump a eșuat (container=" + container + "): " + err.trim());
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IOException("Export PostgreSQL (docker) întrerupt.", ex);
        }
    }

    private void runDockerPsql(String host, String port, String user, String pass, String dbName, Path inputSql) throws IOException {
        String container = resolveDockerContainerName();
        if (container == null || container.isBlank()) {
            throw new IOException("Nu găsesc 'psql' local și nici numele containerului Docker. Setează DB_DOCKER_CONTAINER.");
        }
        List<String> cmd = Arrays.asList(
                "docker", "exec", "-i", "-e", "PGPASSWORD=" + (pass == null ? "" : pass), container,
                "psql", "-h", host, "-p", port, "-U", user, "-d", dbName);
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process p = pb.start();
        try (OutputStream stdin = p.getOutputStream();
             InputStream sql = Files.newInputStream(inputSql)) {
            sql.transferTo(stdin);
        }
        byte[] mergedOutput = p.getInputStream().readAllBytes();
        try {
            int code = waitForProcess(p, "docker exec psql");
            if (code != 0) {
                String err = new String(mergedOutput);
                throw new IOException("docker exec psql a eșuat (container=" + container + "): " + err.trim());
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IOException("Import PostgreSQL (docker) întrerupt.", ex);
        }
    }

    private String resolveDockerContainerName() {
        String[] explicit = new String[] {
                environment.getProperty("DB_DOCKER_CONTAINER"),
                environment.getProperty("POSTGRES_DOCKER_CONTAINER"),
                environment.getProperty("ministryadmin.db-docker-container")
        };
        for (String value : explicit) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        String dbName = extractPostgresDbName(environment.getProperty("spring.datasource.url", ""));
        String[] candidates = new String[] { dbName, "church_db_container", "postgres", "postgresql" };
        for (String candidate : candidates) {
            if (dockerContainerExists(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private boolean dockerContainerExists(String name) {
        try {
            ProcessBuilder pb = new ProcessBuilder("docker", "inspect", name);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            p.getInputStream().transferTo(OutputStream.nullOutputStream());
            int code = waitForProcess(p, "docker inspect");
            return code == 0;
        } catch (Exception ex) {
            return false;
        }
    }

    private int waitForProcess(Process process, String operation) throws IOException, InterruptedException {
        boolean finished = process.waitFor(PROCESS_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new IOException(operation + " a depășit timeout-ul de " + PROCESS_TIMEOUT_SECONDS + "s.");
        }
        return process.exitValue();
    }

    private Path resolveH2BasePath(String jdbcUrl) {
        String raw = jdbcUrl.substring("jdbc:h2:file:".length());
        int paramsIdx = raw.indexOf(';');
        String base = paramsIdx >= 0 ? raw.substring(0, paramsIdx) : raw;
        if (base.startsWith("~")) {
            base = System.getProperty("user.home") + base.substring(1);
        }
        return Paths.get(base).toAbsolutePath().normalize();
    }

    private List<Path> listH2Files(Path base) throws IOException {
        List<Path> files = new ArrayList<>();
        Path parent = base.getParent() == null ? Paths.get(".") : base.getParent();
        String stem = base.getFileName().toString();
        if (!Files.exists(parent)) {
            return files;
        }
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(parent, stem + "*")) {
            for (Path p : ds) {
                if (!Files.isRegularFile(p)) {
                    continue;
                }
                String name = p.getFileName().toString();
                if (name.endsWith(".mv.db") || name.endsWith(".trace.db") || name.endsWith(".h2.db")) {
                    files.add(p);
                }
            }
        }
        return files;
    }

    private String extractPostgresDbName(String jdbcUrl) {
        int slash = jdbcUrl.lastIndexOf('/');
        if (slash < 0 || slash + 1 >= jdbcUrl.length()) {
            return "postgres";
        }
        String dbPart = jdbcUrl.substring(slash + 1);
        int params = dbPart.indexOf('?');
        return (params >= 0 ? dbPart.substring(0, params) : dbPart).trim();
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
