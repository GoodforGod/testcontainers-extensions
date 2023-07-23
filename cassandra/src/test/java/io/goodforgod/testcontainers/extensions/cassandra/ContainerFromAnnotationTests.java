package io.goodforgod.testcontainers.extensions.cassandra;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.goodforgod.testcontainers.extensions.ContainerMode;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.CassandraContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.utility.DockerImageName;

@TestcontainersCassandra(mode = ContainerMode.PER_METHOD, image = "cassandra:4.1")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ContainerFromAnnotationTests {

    @ContainerCassandra
    private static final CassandraContainer<?> container = new CassandraContainer<>(DockerImageName.parse("cassandra:4.1"))
            .withEnv("CASSANDRA_DC", "mydc")
            .withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger(CassandraContainer.class)))
            .withNetwork(Network.SHARED);

    @Test
    void checkParams(@ContainerCassandraConnection CassandraConnection connection) {
        assertEquals("mydc", connection.params().datacenter());
    }

    @Test
    void checkParamsAgain(@ContainerCassandraConnection CassandraConnection connection) {
        assertEquals("mydc", connection.params().datacenter());
    }
}
