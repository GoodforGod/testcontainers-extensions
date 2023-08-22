package io.goodforgod.testcontainers.extensions.kafka;

import static org.junit.jupiter.api.Assertions.*;

import io.goodforgod.testcontainers.extensions.ContainerMode;
import org.junit.jupiter.api.*;

@TestcontainersKafka(mode = ContainerMode.PER_METHOD, topics = @Topics(value = "my-topic"))
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class KafkaContainerPerClassConstructorTests {

    private final KafkaConnection sameConnection;

    private static KafkaConnection firstConnection;

    KafkaContainerPerClassConstructorTests(@ContainerKafkaConnection KafkaConnection sameConnection) {
        this.sameConnection = sameConnection;
        assertNotNull(sameConnection);
    }

    @Order(1)
    @Test
    void firstConnection(@ContainerKafkaConnection KafkaConnection connection) {
        assertNull(firstConnection);
        assertNotNull(connection);
        assertNotNull(sameConnection);
        assertEquals(sameConnection, connection);
        firstConnection = connection;
    }

    @Order(2)
    @Test
    void secondConnection(@ContainerKafkaConnection KafkaConnection connection) {
        assertNotNull(connection);
        assertNotNull(firstConnection);
        assertNotNull(sameConnection);
        assertEquals(sameConnection, connection);
        assertNotEquals(firstConnection, connection);
    }
}
