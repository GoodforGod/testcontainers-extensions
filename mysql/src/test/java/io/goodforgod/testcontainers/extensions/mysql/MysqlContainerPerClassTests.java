package io.goodforgod.testcontainers.extensions.mysql;

import static org.junit.jupiter.api.Assertions.*;

import io.goodforgod.testcontainers.extensions.ContainerMode;
import io.goodforgod.testcontainers.extensions.jdbc.ContainerMysqlConnection;
import io.goodforgod.testcontainers.extensions.jdbc.JdbcConnection;
import io.goodforgod.testcontainers.extensions.jdbc.TestcontainersMysql;
import org.junit.jupiter.api.*;

@TestcontainersMysql(mode = ContainerMode.PER_CLASS, image = "mysql:8.0-debian")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MysqlContainerPerClassTests {

    @ContainerMysqlConnection
    private JdbcConnection sameConnection;

    private static JdbcConnection firstConnection;

    @Order(1)
    @Test
    void firstConnection(@ContainerMysqlConnection JdbcConnection connection) {
        assertNull(firstConnection);
        assertNotNull(connection);
        assertNotNull(connection.params().jdbcUrl());
        assertNotNull(sameConnection);
        assertEquals(sameConnection, connection);
        firstConnection = connection;
        connection.execute("SELECT 1;");
    }

    @Order(2)
    @Test
    void secondConnection(@ContainerMysqlConnection JdbcConnection connection) {
        assertNotNull(connection);
        assertNotNull(connection.params().jdbcUrl());
        assertNotNull(firstConnection);
        assertNotNull(sameConnection);
        assertEquals(sameConnection, connection);
        assertEquals(firstConnection, connection);
        connection.execute("SELECT 1;");
    }
}
