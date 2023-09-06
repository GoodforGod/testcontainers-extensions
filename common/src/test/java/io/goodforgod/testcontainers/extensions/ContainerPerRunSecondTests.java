package io.goodforgod.testcontainers.extensions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.goodforgod.testcontainers.extensions.example.ContainerRedisConnection;
import io.goodforgod.testcontainers.extensions.example.RedisConnection;
import io.goodforgod.testcontainers.extensions.example.TestcontainersRedis;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@TestcontainersRedis(mode = ContainerMode.PER_RUN)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ContainerPerRunSecondTests {

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

        if (ContainerPerRunFirstTests.perRunConnection != null) {
            assertEquals(perRunConnection, ContainerPerRunFirstTests.perRunConnection);
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
