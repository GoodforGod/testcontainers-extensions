package io.goodforgod.testcontainers.extensions.kafka;

import static org.junit.jupiter.api.Assertions.*;

import io.goodforgod.testcontainers.extensions.ContainerMode;
import org.junit.jupiter.api.*;

@TestcontainersKafka(mode = ContainerMode.PER_CLASS, topics = @Topics(value = "my-topic"))
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class KafkaContainerPerClassConstructorTests {

    private final KafkaConnection sameConnection;

    private static KafkaConnection firstConnection;

    KafkaContainerPerClassConstructorTests(@ConnectionKafka KafkaConnection sameConnection) {
        this.sameConnection = sameConnection;
        assertNotNull(sameConnection);
    }

    @BeforeAll
    public static void setupAll(@ConnectionKafka KafkaConnection paramConnection) {
        var consumer = paramConnection.subscribe("my-topic");
        paramConnection.send("my-topic", Event.ofValue("my-value"));
        consumer.assertReceivedAtLeast(1);
        assertNotNull(paramConnection);
    }

    @BeforeEach
    public void setupEach(@ConnectionKafka KafkaConnection paramConnection) {
        var consumer = paramConnection.subscribe("my-topic");
        paramConnection.send("my-topic", Event.ofValue("my-value"));
        consumer.assertReceivedAtLeast(1);
        assertNotNull(paramConnection);
    }

    @Order(1)
    @Test
    void firstConnection(@ConnectionKafka KafkaConnection connection) {
        assertNull(firstConnection);
        assertNotNull(connection);
        assertNotNull(sameConnection);
        assertEquals(sameConnection, connection);
        firstConnection = connection;
    }

    @Order(2)
    @Test
    void secondConnection(@ConnectionKafka KafkaConnection connection) {
        assertNotNull(connection);
        assertNotNull(firstConnection);
        assertNotNull(sameConnection);
        assertEquals(sameConnection, connection);
        assertEquals(firstConnection, connection);
    }
}
