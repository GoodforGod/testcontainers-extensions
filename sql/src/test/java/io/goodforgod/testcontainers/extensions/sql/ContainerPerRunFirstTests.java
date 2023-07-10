package io.goodforgod.testcontainers.extensions.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.*;

@TestcontainersSQL(mode = ContainerMode.PER_RUN, image = "postgres:15.3-alpine")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ContainerPerRunFirstTests {

    static volatile SqlConnection perRunConnection;

    @ContainerSQLConnection
    private SqlConnection sameConnection;

    @Order(1)
    @Test
    void firstConnection(@ContainerSQLConnection SqlConnection connection) {
        assertNotNull(connection);
        assertNotNull(connection.jdbcUrl());
        assertNotNull(sameConnection);
        assertEquals(sameConnection, connection);
        if (perRunConnection == null) {
            perRunConnection = connection;
        }

        if (ContainerPerRunSecondTests.perRunConnection != null) {
            assertEquals(perRunConnection, ContainerPerRunSecondTests.perRunConnection);
        }
    }

    @Order(2)
    @Test
    void secondConnection(@ContainerSQLConnection SqlConnection connection) {
        assertNotNull(connection);
        assertNotNull(connection.jdbcUrl());
        assertNotNull(sameConnection);
        assertEquals(sameConnection, connection);
        assertEquals(perRunConnection, connection);
    }
}
