package io.goodforgod.testcontainers.extensions.redis;

import static org.junit.jupiter.api.Assertions.*;

import io.goodforgod.testcontainers.extensions.ContainerMode;
import org.junit.jupiter.api.*;

@TestcontainersRedis(mode = ContainerMode.PER_METHOD, image = "redis:7.0-alpine")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ContainerPerMethodTests {

    @ContainerRedisConnection
    private RedisConnection samePerMethodConnection;

    private static RedisConnection firstConnection;

    @Order(1)
    @Test
    void firstConnection(@ContainerRedisConnection RedisConnection connection) {
        assertNull(firstConnection);
        assertNotNull(connection);
        firstConnection = connection;
        assertNotNull(samePerMethodConnection);
        assertEquals(samePerMethodConnection, connection);
    }

    @Order(2)
    @Test
    void secondConnection(@ContainerRedisConnection RedisConnection connection) {
        assertNotNull(connection);
        assertNotNull(samePerMethodConnection);
        assertEquals(samePerMethodConnection, connection);
        assertNotNull(firstConnection);
        assertNotEquals(firstConnection, connection);
    }
}
