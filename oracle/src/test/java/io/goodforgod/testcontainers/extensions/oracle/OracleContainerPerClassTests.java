package io.goodforgod.testcontainers.extensions.oracle;

import static org.junit.jupiter.api.Assertions.*;

import io.goodforgod.testcontainers.extensions.ContainerMode;
import io.goodforgod.testcontainers.extensions.jdbc.ContainerOracleConnection;
import io.goodforgod.testcontainers.extensions.jdbc.JdbcConnection;
import io.goodforgod.testcontainers.extensions.jdbc.TestcontainersOracle;
import org.junit.jupiter.api.*;

@TestcontainersOracle(mode = ContainerMode.PER_CLASS, image = "gvenzl/oracle-xe:18.4.0-faststart")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class OracleContainerPerClassTests {

    @ContainerOracleConnection
    private JdbcConnection sameConnection;

    private static JdbcConnection firstConnection;

    @Order(1)
    @Test
    void firstConnection(@ContainerOracleConnection JdbcConnection connection) {
        assertNull(firstConnection);
        assertNotNull(connection);
        assertNotNull(connection.params().jdbcUrl());
        assertNotNull(sameConnection);
        assertEquals(sameConnection, connection);
        firstConnection = connection;
        connection.execute("SELECT 1 FROM DUAL");
    }

    @Order(2)
    @Test
    void secondConnection(@ContainerOracleConnection JdbcConnection connection) {
        assertNotNull(connection);
        assertNotNull(connection.params().jdbcUrl());
        assertNotNull(firstConnection);
        assertNotNull(sameConnection);
        assertEquals(sameConnection, connection);
        assertEquals(firstConnection, connection);
        connection.execute("SELECT 1 FROM DUAL");
    }
}
