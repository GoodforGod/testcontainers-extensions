package io.goodforgod.testcontainers.extensions.jdbc;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.goodforgod.testcontainers.extensions.ContainerMode;
import io.goodforgod.testcontainers.extensions.jdbc.example.ContainerJdbcConnection;
import io.goodforgod.testcontainers.extensions.jdbc.example.TestcontainersJdbc;
import org.junit.jupiter.api.*;

@TestcontainersJdbc(mode = ContainerMode.PER_CLASS,
        image = "postgres:15.1-alpine",
        migration = @Migration(
                engine = Migration.Engines.FLYWAY,
                apply = Migration.Mode.PER_METHOD,
                drop = Migration.Mode.PER_METHOD))
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FlywayPerMethodMigrationTests {

    @BeforeAll
    public static void setupAll(@ContainerJdbcConnection JdbcConnection paramConnection) {
        paramConnection.queryOne("SELECT * FROM users;", r -> r.getInt(1));
        assertNotNull(paramConnection);
    }

    @BeforeEach
    public void setupEach(@ContainerJdbcConnection JdbcConnection paramConnection) {
        paramConnection.queryOne("SELECT * FROM users;", r -> r.getInt(1));
        assertNotNull(paramConnection);
    }

    @Order(1)
    @Test
    void firstRun(@ContainerJdbcConnection JdbcConnection connection) {
        connection.execute("INSERT INTO users VALUES(1);");
    }

    @Order(2)
    @Test
    void secondRun(@ContainerJdbcConnection JdbcConnection connection) {
        var usersFound = connection.queryOne("SELECT * FROM users;", r -> r.getInt(1));
        assertTrue(usersFound.isEmpty());
    }
}
