package io.goodforgod.testcontainers.extensions.postgres;

import static org.junit.jupiter.api.Assertions.*;

import io.goodforgod.testcontainers.extensions.jdbc.ContainerJdbcConnection;
import io.goodforgod.testcontainers.extensions.jdbc.ContainerMode;
import io.goodforgod.testcontainers.extensions.jdbc.JdbcConnection;
import io.goodforgod.testcontainers.extensions.jdbc.TestcontainersPostgres;
import org.junit.jupiter.api.*;

@TestcontainersPostgres(mode = ContainerMode.PER_CLASS, image = "postgres:15.2-alpine")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PostgresContainerPerClassTests {

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
