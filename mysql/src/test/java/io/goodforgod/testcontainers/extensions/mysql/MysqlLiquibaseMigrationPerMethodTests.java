package io.goodforgod.testcontainers.extensions.mysql;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.goodforgod.testcontainers.extensions.jdbc.*;
import org.junit.jupiter.api.*;

@TestcontainersMysql(mode = ContainerMode.PER_CLASS,
        image = "mysql:8.0-debian",
        migration = @Migration(
                engine = Migration.Engines.LIQUIBASE,
                apply = Migration.Mode.PER_METHOD,
                drop = Migration.Mode.PER_METHOD))
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MysqlLiquibaseMigrationPerMethodTests {

    @Order(1)
    @Test
    void firstRun(@ContainerMysqlConnection JdbcConnection connection) {
        connection.execute("INSERT INTO users VALUES(1);");
    }

    @Order(2)
    @Test
    void secondRun(@ContainerMysqlConnection JdbcConnection connection) {
        var usersFound = connection.queryOne("SELECT * FROM users;", r -> r.getInt(1));
        assertTrue(usersFound.isEmpty());
    }
}
