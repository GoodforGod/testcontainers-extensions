package io.goodforgod.testcontainers.extensions.kafka;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.goodforgod.testcontainers.extensions.ContainerMode;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@TestcontainersKafka(mode = ContainerMode.PER_RUN)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class KafkaContainerPerRunSecondTests {

    static volatile KafkaConnection perRunConnection;

    @ContainerKafkaConnection
    private KafkaConnection sameConnection;

    @Order(1)
    @Test
    void firstConnection(@ContainerKafkaConnection KafkaConnection connection) {
        assertNotNull(connection);
        assertNotNull(connection.params().bootstrapServers());
        assertNotNull(sameConnection);
        assertEquals(sameConnection, connection);

        if (perRunConnection == null) {
            perRunConnection = connection;
        }

        if (KafkaContainerPerRunFirstTests.perRunConnection != null) {
            assertEquals(perRunConnection, KafkaContainerPerRunFirstTests.perRunConnection);
        }

        connection.send("my-topic", Event.ofValue("my-value"));
        sameConnection.send("my-topic", Event.ofValue("my-value"));
    }

    @Order(2)
    @Test
    void secondConnection(@ContainerKafkaConnection KafkaConnection connection) {
        assertNotNull(connection);
        assertNotNull(connection.params().bootstrapServers());
        assertNotNull(sameConnection);
        assertEquals(sameConnection, connection);
        assertEquals(perRunConnection, connection);

        connection.send("my-topic", Event.ofValue("my-value"));
        sameConnection.send("my-topic", Event.ofValue("my-value"));
    }
}
