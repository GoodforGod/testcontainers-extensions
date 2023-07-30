package io.goodforgod.testcontainers.extensions.redis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.goodforgod.testcontainers.extensions.ContainerMode;
import org.junit.jupiter.api.*;

@TestcontainersRedis(mode = ContainerMode.PER_RUN, image = "redis:7.0-alpine")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ContainerPerRunSecondTests {

    static volatile RedisConnection perRunConnection;

    @ContainerRedisConnection
    private RedisConnection sameConnection;

    @Order(1)
    @Test
    void firstConnection(@ContainerRedisConnection RedisConnection connection) {
        assertNotNull(connection);
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
    void secondConnection(@ContainerRedisConnection RedisConnection connection) {
        assertNotNull(connection);
        assertNotNull(sameConnection);
        assertEquals(sameConnection, connection);
        assertEquals(perRunConnection, connection);
    }
}
