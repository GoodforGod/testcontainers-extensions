package io.goodforgod.testcontainers.extensions.nats;

import static org.junit.jupiter.api.Assertions.*;

import io.goodforgod.testcontainers.extensions.ContainerMode;
import org.junit.jupiter.api.*;

@TestcontainersNats(mode = ContainerMode.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NatsContainerPerClassConstructorTests {

    private final NatsConnection sameConnection;

    private static NatsConnection firstConnection;

    NatsContainerPerClassConstructorTests(@ConnectionNats NatsConnection sameConnection) {
        this.sameConnection = sameConnection;
        assertNotNull(sameConnection);
    }

    @BeforeAll
    public static void setupAll(@ConnectionNats NatsConnection paramConnection) {
        var subscriber = paramConnection.subscribe("my-subject");
        paramConnection.send("my-subject", Event.ofValue("my-value"));
        subscriber.assertReceivedAtLeast(1);
        assertNotNull(paramConnection);
    }

    @BeforeEach
    public void setupEach(@ConnectionNats NatsConnection paramConnection) {
        var subscriber = paramConnection.subscribe("my-subject");
        paramConnection.send("my-subject", Event.ofValue("my-value"));
        subscriber.assertReceivedAtLeast(1);
        assertNotNull(paramConnection);
    }

    @Order(1)
    @Test
    void firstConnection(@ConnectionNats NatsConnection connection) {
        assertNull(firstConnection);
        assertNotNull(connection);
        assertNotNull(sameConnection);
        assertEquals(sameConnection, connection);
        firstConnection = connection;
    }

    @Order(2)
    @Test
    void secondConnection(@ConnectionNats NatsConnection connection) {
        assertNotNull(connection);
        assertNotNull(firstConnection);
        assertNotNull(sameConnection);
        assertEquals(sameConnection, connection);
        assertEquals(firstConnection, connection);
    }
}
