package io.goodforgod.testcontainers.extensions.postgres;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.goodforgod.testcontainers.extensions.jdbc.*;
import org.junit.jupiter.api.*;

@TestcontainersPostgres(mode = ContainerMode.PER_CLASS,
        image = "postgres:15.2-alpine",
        migration = @Migration(
                engine = Migration.Engines.LIQUIBASE,
                apply = Migration.Mode.PER_METHOD,
                drop = Migration.Mode.PER_METHOD))
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PostgresLiquibaseMigrationPerMethodTests {

    @Order(1)
    @Test
    void firstRun(@ContainerPostgresConnection JdbcConnection connection) {
        connection.execute("INSERT INTO users VALUES(1);");
    }

    @Order(2)
    @Test
    void secondRun(@ContainerPostgresConnection JdbcConnection connection) {
        var usersFound = connection.queryOne("SELECT * FROM users;", r -> r.getInt(1));
        assertTrue(usersFound.isEmpty());
    }
}
