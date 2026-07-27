package io.goodforgod.testcontainers.extensions.arangodb;

import static org.junit.jupiter.api.Assertions.*;

import io.goodforgod.testcontainers.extensions.ContainerMode;
import io.testcontainers.arangodb.containers.ArangoContainer;
import org.junit.jupiter.api.Test;

@TestcontainersArango(mode = ContainerMode.PER_CLASS, image = "arangodb:3.12.4")
class ArangoConnectionAssertsTests {

    @ConnectionArango
    private ArangoConnection connection;

    @Test
    void connectionParams() {
        assertNotNull(connection);
        assertNotNull(connection.client());
        assertEquals("root", connection.params().username());
        assertTrue(connection.params().port() > 0);
        assertEquals(ArangoContainer.PORT, connection.paramsInNetwork().orElseThrow().port());
    }
}
