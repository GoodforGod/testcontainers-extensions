package io.goodforgod.testcontainers.extensions.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.*;

@TestcontainersSQL(mode = ContainerMode.PER_CLASS,
        image = "postgres:15.2-alpine",
        migration = @Migration(
                engine = Migration.Engines.LIQUIBASE,
                apply = Migration.Mode.PER_CLASS,
                drop = Migration.Mode.PER_CLASS))
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LiquibaseMigrationPerClassTests {

    @Order(1)
    @Test
    void firstRun(@ContainerSqlConnection SqlConnection connection) {
        connection.execute("INSERT INTO users VALUES(1);");
    }

    @Order(2)
    @Test
    void secondRun(@ContainerSqlConnection SqlConnection connection) {
        var usersFound = connection.queryOne("SELECT * FROM users;", r -> r.getInt(1)).orElse(null);
        assertEquals(1, usersFound);
    }
}
