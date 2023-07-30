package io.goodforgod.testcontainers.extensions.postgres;

import static org.junit.jupiter.api.Assertions.*;

import io.goodforgod.testcontainers.extensions.ContainerMode;
import io.goodforgod.testcontainers.extensions.jdbc.ContainerPostgresConnection;
import io.goodforgod.testcontainers.extensions.jdbc.JdbcConnection;
import io.goodforgod.testcontainers.extensions.jdbc.TestcontainersPostgres;
import org.junit.jupiter.api.*;

@TestcontainersPostgres(mode = ContainerMode.PER_CLASS, image = "postgres:15.3-alpine")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PostgresContainerPerClassTests {

    @ContainerPostgresConnection
    private JdbcConnection sameConnection;

    private static JdbcConnection firstConnection;

    @Order(1)
    @Test
    void firstConnection(@ContainerPostgresConnection JdbcConnection connection) {
        assertNull(firstConnection);
        assertNotNull(connection);
        assertNotNull(connection.params().jdbcUrl());
        assertNotNull(sameConnection);
        assertEquals(sameConnection, connection);
        firstConnection = connection;
    }

    @Order(2)
    @Test
    void secondConnection(@ContainerPostgresConnection JdbcConnection connection) {
        assertNotNull(connection);
        assertNotNull(connection.params().jdbcUrl());
        assertNotNull(firstConnection);
        assertNotNull(sameConnection);
        assertEquals(sameConnection, connection);
        assertEquals(firstConnection, connection);
    }
}
