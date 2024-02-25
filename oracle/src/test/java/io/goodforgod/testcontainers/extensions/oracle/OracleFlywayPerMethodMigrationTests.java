package io.goodforgod.testcontainers.extensions.oracle;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.goodforgod.testcontainers.extensions.ContainerMode;
import io.goodforgod.testcontainers.extensions.jdbc.ConnectionOracle;
import io.goodforgod.testcontainers.extensions.jdbc.JdbcConnection;
import io.goodforgod.testcontainers.extensions.jdbc.Migration;
import io.goodforgod.testcontainers.extensions.jdbc.TestcontainersOracle;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@TestcontainersOracle(mode = ContainerMode.PER_CLASS,
        image = "gvenzl/oracle-xe:18.4.0-faststart",
        migration = @Migration(
                engine = Migration.Engines.FLYWAY,
                apply = Migration.Mode.PER_METHOD,
                drop = Migration.Mode.PER_METHOD))
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class OracleFlywayPerMethodMigrationTests {

    @Order(1)
    @Test
    void firstRun(@ConnectionOracle JdbcConnection connection) {
        connection.execute("INSERT INTO users VALUES(1)");
    }

    @Order(2)
    @Test
    void secondRun(@ConnectionOracle JdbcConnection connection) {
        var usersFound = connection.queryOne("SELECT * FROM users", r -> r.getInt(1));
        assertTrue(usersFound.isEmpty());
    }
}
