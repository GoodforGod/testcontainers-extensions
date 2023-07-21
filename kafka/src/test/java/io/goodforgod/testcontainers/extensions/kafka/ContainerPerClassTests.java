package io.goodforgod.testcontainers.extensions.kafka;

import static org.junit.jupiter.api.Assertions.*;

import io.goodforgod.testcontainers.extensions.ContainerMode;
import org.junit.jupiter.api.*;

@TestcontainersKafka(mode = ContainerMode.PER_CLASS, image = "confluentinc/cp-kafka:7.4.1")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ContainerPerClassTests {

    @ContainerKafkaConnection
    private KafkaConnection sameConnection;

    private static KafkaConnection firstConnection;

    @Order(1)
    @Test
    void firstConnection(@ContainerKafkaConnection KafkaConnection connection) {
        assertNull(firstConnection);
        assertNotNull(connection);
        assertNotNull(connection.properties());
        assertNotNull(sameConnection);
        assertEquals(sameConnection, connection);
        assertEquals(sameConnection.toString(), connection.toString());
        firstConnection = connection;
    }

    @Order(2)
    @Test
    void secondConnection(@ContainerKafkaConnection KafkaConnection connection) {
        assertNotNull(connection);
        assertNotNull(connection.properties());
        assertNotNull(firstConnection);
        assertNotNull(sameConnection);
        assertEquals(sameConnection, connection);
        assertEquals(firstConnection, connection);
    }
}