package io.goodforgod.testcontainers.extensions.sql;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@TestcontainersSQL(mode = ContainerMode.PER_RUN, image = "postgres:15.3-alpine")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ContainerPerRunSecondTests {

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

        if(perRunConnection == null) {
            perRunConnection = connection;
        }

        if(ContainerPerRunFirstTests.perRunConnection != null) {
            assertEquals(perRunConnection, ContainerPerRunFirstTests.perRunConnection);
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