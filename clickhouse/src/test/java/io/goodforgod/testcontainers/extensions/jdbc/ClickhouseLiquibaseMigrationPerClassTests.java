package io.goodforgod.testcontainers.extensions.jdbc;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.goodforgod.testcontainers.extensions.ContainerMode;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@TestcontainersClickhouse(mode = ContainerMode.PER_CLASS,
        image = "clickhouse/clickhouse-server:24.4.1-alpine",
        migration = @Migration(
                engine = Migration.Engines.LIQUIBASE,
                apply = Migration.Mode.PER_CLASS,
                drop = Migration.Mode.PER_CLASS))
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ClickhouseLiquibaseMigrationPerClassTests {

    @Order(1)
    @Test
    void firstRun(@ConnectionClickhouse JdbcConnection connection) {
        connection.execute("INSERT INTO users VALUES(1);");
    }

    @Order(2)
    @Test
    void secondRun(@ConnectionClickhouse JdbcConnection connection) {
        var usersFound = connection.queryMany("SELECT * FROM users;", r -> r.getInt(1));
        assertEquals(1, usersFound.size());
    }
}
