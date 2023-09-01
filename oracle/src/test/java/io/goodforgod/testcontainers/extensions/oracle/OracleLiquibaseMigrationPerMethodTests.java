package io.goodforgod.testcontainers.extensions.oracle;

import io.goodforgod.testcontainers.extensions.ContainerMode;
import io.goodforgod.testcontainers.extensions.jdbc.ContainerOracleConnection;
import io.goodforgod.testcontainers.extensions.jdbc.JdbcConnection;
import io.goodforgod.testcontainers.extensions.jdbc.Migration;
import io.goodforgod.testcontainers.extensions.jdbc.TestcontainersOracle;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static org.junit.jupiter.api.Assertions.assertTrue;

@TestcontainersOracle(mode = ContainerMode.PER_CLASS,
        image = "gvenzl/oracle-xe:18.4.0-faststart",
        migration = @Migration(
                engine = Migration.Engines.LIQUIBASE,
                apply = Migration.Mode.PER_METHOD,
                drop = Migration.Mode.PER_METHOD))
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class OracleLiquibaseMigrationPerMethodTests {

    @Order(1)
    @Test
    void firstRun(@ContainerOracleConnection JdbcConnection connection) {
        connection.execute("INSERT INTO users VALUES(1)");
    }

    @Order(2)
    @Test
    void secondRun(@ContainerOracleConnection JdbcConnection connection) {
        var usersFound = connection.queryOne("SELECT * FROM users", r -> r.getInt(1));
        assertTrue(usersFound.isEmpty());
    }
}
