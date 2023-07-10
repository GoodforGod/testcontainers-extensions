package io.goodforgod.testcontainers.extensions.sql;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestcontainersSQL(mode = ContainerMode.PER_CLASS,
    image = "postgres:15.2-alpine",
    migration = @Migration(
        engine = Migration.Engines.FLYWAY,
        apply = Migration.Mode.PER_CLASS,
        drop = Migration.Mode.PER_CLASS
    ))
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FlywayPerClassMigrationTests {

    @Order(1)
    @Test
    void firstRun(@ContainerSQLConnection SqlConnection connection) {
        connection.execute("INSERT INTO users VALUES(1);");
    }

    @Order(2)
    @Test
    void secondRun(@ContainerSQLConnection SqlConnection connection) {
        var usersFound = connection.executeForOne("SELECT * FROM users;", r -> r.getInt(1)).orElse(null);;
        assertEquals(1, usersFound);
    }
}
