package io.goodforgod.testcontainers.extensions.nats;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.goodforgod.testcontainers.extensions.ContainerMode;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@TestcontainersNats(mode = ContainerMode.PER_RUN)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class NatsContainerPerRunFirstTests {

    static volatile NatsConnection perRunConnection;

    @ConnectionNats
    private NatsConnection sameConnection;

    @Order(1)
    @Test
    void firstConnection(@ConnectionNats NatsConnection connection) {
        assertNotNull(connection);
        assertNotNull(connection.params().url());
        assertNotNull(sameConnection);
        assertEquals(sameConnection, connection);

        if (perRunConnection == null) {
            perRunConnection = connection;
        }

        if (NatsContainerPerRunSecondTests.perRunConnection != null) {
            assertEquals(perRunConnection, NatsContainerPerRunSecondTests.perRunConnection);
        }

        connection.send("my-subject", Event.ofValue("my-value"));
        sameConnection.send("my-subject", Event.ofValue("my-value"));
    }

    @Order(2)
    @Test
    void secondConnection(@ConnectionNats NatsConnection connection) {
        assertNotNull(connection);
        assertNotNull(connection.params().url());
        assertNotNull(sameConnection);
        assertEquals(sameConnection, connection);
        assertEquals(perRunConnection, connection);

        connection.send("my-subject", Event.ofValue("my-value"));
        sameConnection.send("my-subject", Event.ofValue("my-value"));
    }
}
