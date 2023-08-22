package io.goodforgod.testcontainers.extensions.kafka;

import static org.junit.jupiter.api.Assertions.*;

import io.goodforgod.testcontainers.extensions.ContainerMode;
import org.junit.jupiter.api.*;

@TestcontainersKafka(mode = ContainerMode.PER_CLASS, image = "confluentinc/cp-kafka:7.4.1", topics = @Topics("my-topic"))
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ContainerPerClassTests {

    @ContainerKafkaConnection
    private KafkaConnection sameConnection;

    private static KafkaConnection firstConnection;

    @Order(1)
    @Test
    void firstConnection(@ContainerKafkaConnection KafkaConnection connection) {
        assertNull(firstConnection);
        assertNotNull(connection);
        assertNotNull(connection.params().boostrapServers());
        assertNotNull(connection.params().properties());
        assertNotNull(sameConnection);
        assertEquals(sameConnection, connection);
        assertEquals(sameConnection.toString(), connection.toString());
        firstConnection = connection;
    }

    @Order(2)
    @Test
    void secondConnection(@ContainerKafkaConnection KafkaConnection connection) {
        assertNotNull(connection);
        assertNotNull(connection.params().boostrapServers());
        assertNotNull(connection.params().properties());
        assertNotNull(firstConnection);
        assertNotNull(sameConnection);
        assertEquals(sameConnection, connection);
        assertEquals(firstConnection, connection);
    }
}
