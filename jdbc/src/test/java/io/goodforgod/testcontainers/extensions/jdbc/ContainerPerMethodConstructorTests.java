package io.goodforgod.testcontainers.extensions.jdbc;

import static org.junit.jupiter.api.Assertions.*;

import io.goodforgod.testcontainers.extensions.ContainerMode;
import org.junit.jupiter.api.*;

@TestcontainersJdbc(mode = ContainerMode.PER_METHOD, image = "postgres:15.1-alpine")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class ContainerPerMethodConstructorTests {

    private final JdbcConnection diffPerMethod;

    private static JdbcConnection firstConnection;

    ContainerPerMethodConstructorTests(@ContainerJdbcConnection JdbcConnection diffPerMethod) {
        this.diffPerMethod = diffPerMethod;
        assertNotNull(diffPerMethod);
    }

    @Order(1)
    @Test
    void firstConnection(@ContainerJdbcConnection JdbcConnection connection) {
        assertNull(firstConnection);
        assertNotNull(connection);
        assertNotNull(connection.params().jdbcUrl());
        firstConnection = connection;
        assertNotNull(diffPerMethod);
        assertEquals(diffPerMethod, connection);
    }

    @Order(2)
    @Test
    void secondConnection(@ContainerJdbcConnection JdbcConnection connection) {
        assertNotNull(connection);
        assertNotNull(connection.params().jdbcUrl());
        assertNotNull(diffPerMethod);
        assertEquals(diffPerMethod, connection);

        assertNotNull(firstConnection);
        assertNotEquals(firstConnection, connection);
    }
}
