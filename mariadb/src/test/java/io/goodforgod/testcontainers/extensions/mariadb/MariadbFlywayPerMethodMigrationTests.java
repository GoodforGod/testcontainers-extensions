package io.goodforgod.testcontainers.extensions.mariadb;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.goodforgod.testcontainers.extensions.ContainerMode;
import io.goodforgod.testcontainers.extensions.jdbc.ConnectionMariaDB;
import io.goodforgod.testcontainers.extensions.jdbc.JdbcConnection;
import io.goodforgod.testcontainers.extensions.jdbc.Migration;
import io.goodforgod.testcontainers.extensions.jdbc.TestcontainersMariaDB;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@TestcontainersMariaDB(mode = ContainerMode.PER_CLASS,
        image = "mariadb:11.2-jammy",
        migration = @Migration(
                engine = Migration.Engines.FLYWAY,
                apply = Migration.Mode.PER_METHOD,
                drop = Migration.Mode.PER_METHOD))
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MariadbFlywayPerMethodMigrationTests {

    @Order(1)
    @Test
    void firstRun(@ConnectionMariaDB JdbcConnection connection) {
        connection.execute("INSERT INTO users VALUES(1);");
    }

    @Order(2)
    @Test
    void secondRun(@ConnectionMariaDB JdbcConnection connection) {
        var usersFound = connection.queryOne("SELECT * FROM users;", r -> r.getInt(1));
        assertTrue(usersFound.isEmpty());
    }
}
