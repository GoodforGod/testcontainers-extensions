package io.goodforgod.testcontainers.extensions.redpanda;

import static org.junit.jupiter.api.Assertions.*;

import io.goodforgod.testcontainers.extensions.ContainerMode;
import org.junit.jupiter.api.*;

@TestcontainersRedpanda(mode = ContainerMode.PER_METHOD, topics = @Topics(value = "my-topic"))
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class RedpandaContainerPerMethodConstructorTests {

    private final RedpandaConnection sameConnection;

    private static RedpandaConnection firstConnection;

    RedpandaContainerPerMethodConstructorTests(@ConnectionRedpanda RedpandaConnection sameConnection) {
        this.sameConnection = sameConnection;
        assertNotNull(sameConnection);
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
        assertNotEquals(firstConnection, connection);
    }
}
