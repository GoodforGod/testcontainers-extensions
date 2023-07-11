package io.goodforgod.testcontainers.extensions.jdbc;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.*;

@TestcontainersJdbc(mode = ContainerMode.PER_METHOD, image = "postgres:15.1-alpine")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ContainerPerMethodTests {

    @ContainerJdbcConnection
    private JdbcConnection samePerMethodConnection;

    private static JdbcConnection firstConnection;

    @Order(1)
    @Test
    void firstConnection(@ContainerJdbcConnection JdbcConnection connection) {
        assertNull(firstConnection);
        assertNotNull(connection);
        assertNotNull(connection.params().jdbcUrl());
        firstConnection = connection;
        assertNotNull(samePerMethodConnection);
        assertEquals(samePerMethodConnection, connection);
    }

    @Order(2)
    @Test
    void secondConnection(@ContainerJdbcConnection JdbcConnection connection) {
        assertNotNull(connection);
        assertNotNull(connection.params().jdbcUrl());
        assertNotNull(samePerMethodConnection);
        assertEquals(samePerMethodConnection, connection);
        assertNotNull(firstConnection);
        assertNotEquals(firstConnection, connection);
    }
}
