package io.goodforgod.testcontainers.extensions.oracle;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.goodforgod.testcontainers.extensions.jdbc.*;
import org.junit.jupiter.api.*;

@TestcontainersOracle(mode = ContainerMode.PER_CLASS,
        image = "gvenzl/oracle-xe:18.4.0-faststart",
        migration = @Migration(
                engine = Migration.Engines.FLYWAY,
                apply = Migration.Mode.PER_METHOD,
                drop = Migration.Mode.PER_METHOD))
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OracleFlywayPerMethodMigrationTests {

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
