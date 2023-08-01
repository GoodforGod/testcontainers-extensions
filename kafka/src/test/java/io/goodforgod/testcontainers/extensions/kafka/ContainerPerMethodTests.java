package io.goodforgod.testcontainers.extensions.kafka;

import static org.junit.jupiter.api.Assertions.*;

import io.goodforgod.testcontainers.extensions.ContainerMode;
import org.junit.jupiter.api.*;

@TestcontainersKafka(mode = ContainerMode.PER_METHOD, image = "confluentinc/cp-kafka:7.4.1")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ContainerPerMethodTests {

    @ContainerKafkaConnection
    private KafkaConnection samePerMethodConnection;

    private static KafkaConnection firstConnection;

    @Order(1)
    @Test
    void firstConnection(@ContainerKafkaConnection KafkaConnection connection) {
        assertNull(firstConnection);
        assertNotNull(connection);
        assertNotNull(connection.params().boostrapServers());
        assertNotNull(connection.params().properties());
        firstConnection = connection;
        assertNotNull(samePerMethodConnection);
        assertEquals(samePerMethodConnection, connection);
    }

    @Order(2)
    @Test
    void secondConnection(@ContainerKafkaConnection KafkaConnection connection) {
        assertNotNull(connection);
        assertNotNull(connection.params().boostrapServers());
        assertNotNull(connection.params().properties());
        assertNotNull(samePerMethodConnection);
        assertEquals(samePerMethodConnection, connection);
        assertNotNull(firstConnection);
        assertNotEquals(firstConnection, connection);
    }
}
