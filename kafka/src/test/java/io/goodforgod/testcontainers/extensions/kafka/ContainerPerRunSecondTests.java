package io.goodforgod.testcontainers.extensions.kafka;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.goodforgod.testcontainers.extensions.ContainerMode;
import org.junit.jupiter.api.*;

@TestcontainersKafka(mode = ContainerMode.PER_RUN, image = "confluentinc/cp-kafka:7.4.1", topics = { "my-topic" })
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ContainerPerRunSecondTests {

    static volatile KafkaConnection perRunConnection;

    @ContainerKafkaConnection
    private KafkaConnection sameConnection;

    @Order(1)
    @Test
    void firstConnection(@ContainerKafkaConnection KafkaConnection connection) {
        assertNotNull(connection);
        assertNotNull(connection.params().boostrapServers());
        assertNotNull(connection.params().properties());
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
    void secondConnection(@ContainerKafkaConnection KafkaConnection connection) {
        assertNotNull(connection);
        assertNotNull(connection.params().boostrapServers());
        assertNotNull(connection.params().properties());
        assertNotNull(sameConnection);
        assertEquals(sameConnection, connection);
        assertEquals(perRunConnection, connection);
    }
}
