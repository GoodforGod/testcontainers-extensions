package io.goodforgod.testcontainers.extensions.cassandra;

import static org.junit.jupiter.api.Assertions.*;

import io.goodforgod.testcontainers.extensions.ContainerMode;
import io.goodforgod.testcontainers.extensions.arangodb.ArangoConnection;
import io.goodforgod.testcontainers.extensions.arangodb.ConnectionArango;
import io.goodforgod.testcontainers.extensions.arangodb.TestcontainersArango;
import org.junit.jupiter.api.Test;

@TestcontainersArango(mode = ContainerMode.PER_CLASS, image = "arangodb:3.12.4")
@TestcontainersCassandra(mode = ContainerMode.PER_CLASS, image = "cassandra:4.1")
class ParallelArangoCassandraOrchestratorTests {

    @ConnectionArango
    private ArangoConnection arango;

    @ConnectionCassandra
    private CassandraConnection cassandra;

    @Test
    void injectsBothConnections() {
        assertNotNull(arango);
        assertNotNull(cassandra);
        assertNotNull(arango.client());
        assertNotNull(cassandra.getConnection());
    }
}
