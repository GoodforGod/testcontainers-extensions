package io.goodforgod.testcontainers.extensions.jdbc;

import static org.junit.jupiter.api.Assertions.*;

import io.goodforgod.testcontainers.extensions.ContainerMode;
import org.junit.jupiter.api.*;

@TestcontainersCockroachdb(mode = ContainerMode.PER_CLASS, image = "cockroachdb/cockroach:latest-v23.1")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CockroachdbContainerPerClassTests {

    @ContainerCockroachdbConnection
    private JdbcConnection sameConnection;

    private static JdbcConnection firstConnection;

    @Order(1)
    @Test
    void firstConnection(@ContainerCockroachdbConnection JdbcConnection connection) {
        assertNull(firstConnection);
        assertNotNull(connection);
        assertNotNull(connection.params().jdbcUrl());
        assertNotNull(sameConnection);
        assertEquals(sameConnection, connection);
        firstConnection = connection;
    }

    @Order(2)
    @Test
    void secondConnection(@ContainerCockroachdbConnection JdbcConnection connection) {
        assertNotNull(connection);
        assertNotNull(connection.params().jdbcUrl());
        assertNotNull(firstConnection);
        assertNotNull(sameConnection);
        assertEquals(sameConnection, connection);
        assertEquals(firstConnection, connection);
    }
}
