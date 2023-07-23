package io.goodforgod.testcontainers.extensions.jdbc;

import static org.junit.jupiter.api.Assertions.*;

import io.goodforgod.testcontainers.extensions.ContainerMode;
import org.junit.jupiter.api.*;

@TestcontainersJdbc(mode = ContainerMode.PER_CLASS, image = "postgres:15.2-alpine")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ContainerPerClassTests {

    @ContainerJdbcConnection
    private JdbcConnection sameConnection;

    private static JdbcConnection firstConnection;

    @Order(1)
    @Test
    void firstConnection(@ContainerJdbcConnection JdbcConnection connection) {
        assertNull(firstConnection);
        assertNotNull(connection);
        assertNotNull(connection.params().jdbcUrl());
        assertNotNull(sameConnection);
        assertEquals(sameConnection, connection);
        firstConnection = connection;
    }

    @Order(2)
    @Test
    void secondConnection(@ContainerJdbcConnection JdbcConnection connection) {
        assertNotNull(connection);
        assertNotNull(connection.params().jdbcUrl());
        assertNotNull(firstConnection);
        assertNotNull(sameConnection);
        assertEquals(sameConnection, connection);
        assertEquals(firstConnection, connection);
    }
}
