package io.goodforgod.testcontainers.extensions.jdbc;

import io.goodforgod.testcontainers.extensions.ContainerMode;
import io.goodforgod.testcontainers.extensions.jdbc.example.ContainerJdbcConnection;
import io.goodforgod.testcontainers.extensions.jdbc.example.TestcontainersJdbc;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestcontainersJdbc(mode = ContainerMode.PER_CLASS,
        image = "postgres:15.2-alpine",
        migration = @Migration(
                engine = Migration.Engines.FLYWAY,
                apply = Migration.Mode.PER_CLASS,
                drop = Migration.Mode.PER_CLASS))
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FlywayPerClassMigrationTests {

    @Order(1)
    @Test
    void firstRun(@ContainerJdbcConnection JdbcConnection connection) {
        connection.execute("INSERT INTO users VALUES(1);");
    }

    @Order(2)
    @Test
    void secondRun(@ContainerJdbcConnection JdbcConnection connection) {
        var usersFound = connection.queryOne("SELECT * FROM users;", r -> r.getInt(1)).orElse(null);
        assertEquals(1, usersFound);
    }
}
