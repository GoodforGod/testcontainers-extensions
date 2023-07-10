package io.goodforgod.testcontainers.extensions.sql;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.*;

@TestcontainersSQL(mode = ContainerMode.PER_CLASS, image = "postgres:15.2-alpine")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ContainerPerClassTests {

    @ContainerSQLConnection
    private SqlConnection sameConnection;

    private static SqlConnection firstConnection;

    @Order(1)
    @Test
    void firstConnection(@ContainerSQLConnection SqlConnection connection) {
        assertNull(firstConnection);
        assertNotNull(connection);
        assertNotNull(connection.jdbcUrl());
        assertNotNull(sameConnection);
        assertEquals(sameConnection, connection);
        firstConnection = connection;
    }

    @Order(2)
    @Test
    void secondConnection(@ContainerSQLConnection SqlConnection connection) {
        assertNotNull(connection);
        assertNotNull(connection.jdbcUrl());
        assertNotNull(firstConnection);
        assertNotNull(sameConnection);
        assertEquals(sameConnection, connection);
        assertEquals(firstConnection, connection);
    }
}
