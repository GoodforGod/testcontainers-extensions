package io.goodforgod.testcontainers.extensions.jdbc;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.goodforgod.testcontainers.extensions.ContainerMode;
import org.junit.jupiter.api.*;

@TestcontainersCockroachdb(mode = ContainerMode.PER_CLASS,
        image = "cockroachdb/cockroach:latest-v23.1",
        migration = @Migration(
                engine = Migration.Engines.FLYWAY,
                apply = Migration.Mode.PER_METHOD,
                drop = Migration.Mode.PER_METHOD))
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CockroachdbFlywayPerMethodMigrationTests {

    @Order(1)
    @Test
    void firstRun(@ContainerCockroachdbConnection JdbcConnection connection) {
        connection.execute("INSERT INTO users VALUES(1);");
    }

    @Order(2)
    @Test
    void secondRun(@ContainerCockroachdbConnection JdbcConnection connection) {
        var usersFound = connection.queryOne("SELECT * FROM users;", r -> r.getInt(1));
        assertTrue(usersFound.isEmpty());
    }
}
