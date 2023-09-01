package io.goodforgod.testcontainers.extensions;

import io.goodforgod.testcontainers.extensions.example.ContainerRedisConnection;
import io.goodforgod.testcontainers.extensions.example.RedisConnection;
import io.goodforgod.testcontainers.extensions.example.TestcontainersRedis;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@TestcontainersRedis(mode = ContainerMode.PER_RUN, image = "redis:7.2-alpine")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ContainerPerRunFirstTests {

    static volatile RedisConnection perRunConnection;

    @ContainerRedisConnection
    private RedisConnection sameConnection;

    @Order(1)
    @Test
    void firstConnection(@ContainerRedisConnection RedisConnection connection) {
        assertNotNull(connection);
        assertNotNull(connection.params().uri());
        assertNotNull(sameConnection);
        assertEquals(sameConnection, connection);

        if (perRunConnection == null) {
            perRunConnection = connection;
        }

        if (ContainerPerRunSecondTests.perRunConnection != null) {
            assertEquals(perRunConnection, ContainerPerRunSecondTests.perRunConnection);
        }
    }

    @Order(2)
    @Test
    void secondConnection(@ContainerRedisConnection RedisConnection connection) {
        assertNotNull(connection);
        assertNotNull(connection.params().uri());
        assertNotNull(sameConnection);
        assertEquals(sameConnection, connection);
        assertEquals(perRunConnection, connection);
    }
}
