package io.goodforgod.testcontainers.extensions.redpanda;

import static org.junit.jupiter.api.Assertions.*;

import io.goodforgod.testcontainers.extensions.ContainerMode;
import org.junit.jupiter.api.*;

@TestcontainersRedpanda(mode = ContainerMode.PER_CLASS, topics = @Topics(value = "my-topic"))
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RedpandaContainerPerClassConstructorTests {

    private final RedpandaConnection sameConnection;

    private static RedpandaConnection firstConnection;

    RedpandaContainerPerClassConstructorTests(@ConnectionRedpanda RedpandaConnection sameConnection) {
        this.sameConnection = sameConnection;
        assertNotNull(sameConnection);
    }

    @BeforeAll
    public static void setupAll(@ConnectionRedpanda RedpandaConnection paramConnection) {
        var consumer = paramConnection.subscribe("my-topic");
        paramConnection.send("my-topic", Event.ofValue("my-value"));
        consumer.assertReceivedAtLeast(1);
        assertNotNull(paramConnection);
    }

    @BeforeEach
    public void setupEach(@ConnectionRedpanda RedpandaConnection paramConnection) {
        var consumer = paramConnection.subscribe("my-topic");
        paramConnection.send("my-topic", Event.ofValue("my-value"));
        consumer.assertReceivedAtLeast(1);
        assertNotNull(paramConnection);
    }

    @Order(1)
    @Test
    void firstConnection(@ConnectionRedpanda RedpandaConnection connection) {
        assertNull(firstConnection);
        assertNotNull(connection);
        assertNotNull(sameConnection);
        assertEquals(sameConnection, connection);
        firstConnection = connection;
    }

    @Order(2)
    @Test
    void secondConnection(@ConnectionRedpanda RedpandaConnection connection) {
        assertNotNull(connection);
        assertNotNull(firstConnection);
        assertNotNull(sameConnection);
        assertEquals(sameConnection, connection);
        assertEquals(firstConnection, connection);
    }
}
