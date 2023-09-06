package io.goodforgod.testcontainers.extensions;

import static org.junit.jupiter.api.Assertions.*;

import io.goodforgod.testcontainers.extensions.example.ContainerRedisConnection;
import io.goodforgod.testcontainers.extensions.example.RedisConnection;
import io.goodforgod.testcontainers.extensions.example.TestcontainersRedis;
import org.junit.jupiter.api.*;

@TestcontainersRedis(mode = ContainerMode.PER_METHOD)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ContainerPerMethodInstanceClassTests {

    @ContainerRedisConnection
    private RedisConnection samePerMethodConnection;

    private static RedisConnection firstConnection;

    @Order(1)
    @Test
    void firstConnection(@ContainerRedisConnection RedisConnection connection) {
        assertNull(firstConnection);
        assertNotNull(connection);
        assertNotNull(connection.params().uri());
        firstConnection = connection;
        assertNotNull(samePerMethodConnection);
        assertEquals(samePerMethodConnection, connection);
    }

    @Order(2)
    @Test
    void secondConnection(@ContainerRedisConnection RedisConnection connection) {
        assertNotNull(connection);
        assertNotNull(connection.params().uri());
        assertNotNull(samePerMethodConnection);
        assertEquals(samePerMethodConnection, connection);
        assertNotNull(firstConnection);
        assertNotEquals(firstConnection, connection);
    }
}
