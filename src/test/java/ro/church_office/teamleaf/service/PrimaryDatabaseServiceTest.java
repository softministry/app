package ro.church_office.teamleaf.service;

import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import javax.sql.DataSource;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PrimaryDatabaseServiceTest {

    @Test
    void exportThenImportBetweenProjectsWorksForSqliteArchive() throws Exception {
        Path sourceDb = Files.createTempFile("source-", ".sqlite.db");
        Path targetDb = Files.createTempFile("target-", ".sqlite.db");

        seedDatabaseWithWalMode(sourceDb);

        Environment sourceEnv = mock(Environment.class);
        when(sourceEnv.getProperty("spring.datasource.url", ""))
                .thenReturn("jdbc:sqlite:" + sourceDb.toAbsolutePath());
        DataSource dataSource = mock(DataSource.class);

        PrimaryDatabaseService exportService = new PrimaryDatabaseService(dataSource, sourceEnv);

        byte[] zipBytes;
        try (InputStream in = exportService.exportDatabase(false);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            in.transferTo(out);
            zipBytes = out.toByteArray();
        }
        assertTrue(zipBytes.length > 128, "Exportul ar trebui să producă un zip valid, non-gol.");

        Environment targetEnv = mock(Environment.class);
        when(targetEnv.getProperty("spring.datasource.url", ""))
                .thenReturn("jdbc:sqlite:" + targetDb.toAbsolutePath());
        PrimaryDatabaseService importService = new PrimaryDatabaseService(dataSource, targetEnv);

        MultipartFile archive = new MockMultipartFile("file", "db-full.zip", "application/zip", zipBytes);
        importService.importDatabase(archive);

        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + targetDb.toAbsolutePath());
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT count(*) FROM person")) {
            assertTrue(rs.next());
            assertEquals(1, rs.getInt(1));
        }
    }

    private static void seedDatabaseWithWalMode(Path sqliteFile) throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + sqliteFile.toAbsolutePath());
             Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA journal_mode=WAL");
            statement.execute("DROP TABLE IF EXISTS person");
            statement.execute("CREATE TABLE person (id INTEGER PRIMARY KEY, first_name TEXT NOT NULL)");
            statement.execute("INSERT INTO person (first_name) VALUES ('Ana')");
        }
    }
}
