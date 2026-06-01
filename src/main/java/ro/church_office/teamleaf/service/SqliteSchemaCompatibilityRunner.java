package ro.church_office.teamleaf.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SqliteSchemaCompatibilityRunner implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(SqliteSchemaCompatibilityRunner.class);

    private final DataSource dataSource;
    private final Environment environment;

    public SqliteSchemaCompatibilityRunner(DataSource dataSource, Environment environment) {
        this.dataSource = dataSource;
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        String jdbcUrl = environment.getProperty("spring.datasource.url", "");
        if (!jdbcUrl.toLowerCase(Locale.ROOT).startsWith("jdbc:sqlite:")) {
            return;
        }

        try (Connection connection = dataSource.getConnection()) {
            String personTableSql = personTableSql(connection);
            if (personTableSql == null || !usesOrdinalMemberTypeCheck(personTableSql)) {
                return;
            }
            rebuildPersonTableWithStringMemberType(connection);
            log.info("Rebuilt SQLite person table to use string member_type values.");
        }
    }

    private String personTableSql(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT sql FROM sqlite_master WHERE type='table' AND name='person'")) {
            return rs.next() ? rs.getString(1) : null;
        }
    }

    private boolean usesOrdinalMemberTypeCheck(String tableSql) {
        String normalized = tableSql.toLowerCase(Locale.ROOT);
        return normalized.contains("member_type between 0 and 2");
    }

    private void rebuildPersonTableWithStringMemberType(Connection connection) throws SQLException {
        boolean previousAutoCommit = connection.getAutoCommit();
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys=OFF");
            connection.setAutoCommit(false);
            statement.execute("ALTER TABLE person RENAME TO person_legacy_member_type");
            statement.execute("""
                    CREATE TABLE person (
                        id integer primary key autoincrement,
                        address varchar(255),
                        birth_date date,
                        church_id bigint,
                        church_role varchar(255),
                        email varchar(255),
                        first_name varchar(255),
                        last_name varchar(255),
                        member_type varchar(255) check (member_type in ('MEMBER','CHILD','FREND')),
                        phone varchar(255),
                        position varchar(255)
                    )
                    """);
            statement.execute("""
                    INSERT INTO person (
                        id, address, birth_date, church_id, church_role, email, first_name, last_name, member_type, phone, position
                    )
                    SELECT
                        id,
                        address,
                        birth_date,
                        church_id,
                        church_role,
                        email,
                        first_name,
                        last_name,
                        CASE UPPER(CAST(member_type AS TEXT))
                            WHEN '0' THEN 'MEMBER'
                            WHEN '1' THEN 'CHILD'
                            WHEN '2' THEN 'FREND'
                            WHEN 'MEMBER' THEN 'MEMBER'
                            WHEN 'CHILD' THEN 'CHILD'
                            WHEN 'FRIEND' THEN 'FREND'
                            WHEN 'FREND' THEN 'FREND'
                            ELSE 'MEMBER'
                        END,
                        phone,
                        position
                    FROM person_legacy_member_type
                    """);
            statement.execute("DROP TABLE person_legacy_member_type");
            connection.commit();
        } catch (SQLException ex) {
            connection.rollback();
            throw ex;
        } finally {
            connection.setAutoCommit(previousAutoCommit);
            try (Statement statement = connection.createStatement()) {
                statement.execute("PRAGMA foreign_keys=ON");
            }
        }
    }
}
