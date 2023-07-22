package io.goodforgod.testcontainers.extensions.cassandra;

import static org.junit.jupiter.api.Assertions.*;

import io.goodforgod.testcontainers.extensions.ContainerMode;
import org.junit.jupiter.api.*;

@TestcontainersCassandra(mode = ContainerMode.PER_METHOD, image = "cassandra:4.1")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ContainerPerMethodTests {

    @ContainerCassandraConnection
    private CassandraConnection samePerMethodConnection;

    private static CassandraConnection firstConnection;

    @Order(1)
    @Test
    void firstConnection(@ContainerCassandraConnection CassandraConnection connection) {
        assertNull(firstConnection);
        assertNotNull(connection);
        firstConnection = connection;
        assertNotNull(samePerMethodConnection);
        assertEquals(samePerMethodConnection, connection);
    }

    @Order(2)
    @Test
    void secondConnection(@ContainerCassandraConnection CassandraConnection connection) {
        assertNotNull(connection);
        assertNotNull(samePerMethodConnection);
        assertEquals(samePerMethodConnection, connection);
        assertNotNull(firstConnection);
        assertNotEquals(firstConnection, connection);
    }
}
