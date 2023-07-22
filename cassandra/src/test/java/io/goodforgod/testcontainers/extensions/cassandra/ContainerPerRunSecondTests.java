package io.goodforgod.testcontainers.extensions.cassandra;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.goodforgod.testcontainers.extensions.ContainerMode;
import org.junit.jupiter.api.*;

@TestcontainersCassandra(mode = ContainerMode.PER_RUN, image = "cassandra:4.1")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ContainerPerRunSecondTests {

    static volatile CassandraConnection perRunConnection;

    @ContainerCassandraConnection
    private CassandraConnection sameConnection;

    @Order(1)
    @Test
    void firstConnection(@ContainerCassandraConnection CassandraConnection connection) {
        assertNotNull(connection);
        assertNotNull(sameConnection);
        assertEquals(sameConnection, connection);

        if (perRunConnection == null) {
            perRunConnection = connection;
        }

        if (ContainerPerRunFirstTests.perRunConnection != null) {
            assertEquals(perRunConnection, ContainerPerRunFirstTests.perRunConnection);
        }
    }

    @Order(2)
    @Test
    void secondConnection(@ContainerCassandraConnection CassandraConnection connection) {
        assertNotNull(connection);
        assertNotNull(sameConnection);
        assertEquals(sameConnection, connection);
        assertEquals(perRunConnection, connection);
    }
}
