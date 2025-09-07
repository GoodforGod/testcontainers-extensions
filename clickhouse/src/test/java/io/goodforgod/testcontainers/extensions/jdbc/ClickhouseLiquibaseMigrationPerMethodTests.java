package io.goodforgod.testcontainers.extensions.jdbc;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.goodforgod.testcontainers.extensions.ContainerMode;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@TestcontainersClickhouse(mode = ContainerMode.PER_CLASS,
        image = "clickhouse/clickhouse-server:25.8-alpine",
        migration = @Migration(
                engine = Migration.Engines.LIQUIBASE,
                apply = Migration.Mode.PER_METHOD,
                drop = Migration.Mode.PER_METHOD))
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ClickhouseLiquibaseMigrationPerMethodTests {

    @Order(1)
    @Test
    void firstRun(@ConnectionClickhouse JdbcConnection connection) {
        connection.execute("INSERT INTO users VALUES(1);");
    }

    @Order(2)
    @Test
    void secondRun(@ConnectionClickhouse JdbcConnection connection) {
        var usersFound = connection.queryOne("SELECT * FROM users;", r -> r.getInt(1));
        assertTrue(usersFound.isEmpty());
    }
}
