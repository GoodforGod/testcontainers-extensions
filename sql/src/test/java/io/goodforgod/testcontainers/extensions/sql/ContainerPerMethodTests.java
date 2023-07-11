package io.goodforgod.testcontainers.extensions.sql;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.*;

@TestcontainersSQL(mode = ContainerMode.PER_METHOD, image = "postgres:15.1-alpine")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ContainerPerMethodTests {

    @ContainerSqlConnection
    private SqlConnection samePerMethodConnection;

    private static SqlConnection firstConnection;

    @Order(1)
    @Test
    void firstConnection(@ContainerSqlConnection SqlConnection connection) {
        assertNull(firstConnection);
        assertNotNull(connection);
        assertNotNull(connection.jdbcUrl());
        firstConnection = connection;
        assertNotNull(samePerMethodConnection);
        assertEquals(samePerMethodConnection, connection);
    }

    @Order(2)
    @Test
    void secondConnection(@ContainerSqlConnection SqlConnection connection) {
        assertNotNull(connection);
        assertNotNull(connection.jdbcUrl());
        assertNotNull(samePerMethodConnection);
        assertEquals(samePerMethodConnection, connection);
        assertNotNull(firstConnection);
        assertNotEquals(firstConnection, connection);
    }
}
