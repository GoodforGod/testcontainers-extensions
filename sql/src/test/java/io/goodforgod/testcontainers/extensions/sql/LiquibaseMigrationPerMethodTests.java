package io.goodforgod.testcontainers.extensions.sql;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.*;

@TestcontainersSQL(mode = ContainerMode.PER_CLASS,
        image = "postgres:15.1-alpine",
        migration = @Migration(
                engine = Migration.Engines.LIQUIBASE,
                apply = Migration.Mode.PER_METHOD,
                drop = Migration.Mode.PER_METHOD))
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LiquibaseMigrationPerMethodTests {

    @Order(1)
    @Test
    void firstRun(@ContainerSQLConnection SqlConnection connection) {
        connection.execute("INSERT INTO users VALUES(1);");
    }

    @Order(2)
    @Test
    void secondRun(@ContainerSQLConnection SqlConnection connection) {
        var usersFound = connection.executeForOne("SELECT * FROM users;", r -> r.getInt(1));
        assertTrue(usersFound.isEmpty());
    }
}
