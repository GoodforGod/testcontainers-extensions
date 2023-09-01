package io.goodforgod.testcontainers.extensions;

import io.goodforgod.testcontainers.extensions.example.ContainerRedisConnection;
import io.goodforgod.testcontainers.extensions.example.RedisConnection;
import io.goodforgod.testcontainers.extensions.example.TestcontainersRedis;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

@TestcontainersRedis(mode = ContainerMode.PER_METHOD, image = "redis:7.2-alpine")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class ContainerPerMethodConstructorTests {

    private final RedisConnection diffPerMethod;

    private static RedisConnection firstConnection;

    ContainerPerMethodConstructorTests(@ContainerRedisConnection RedisConnection diffPerMethod) {
        this.diffPerMethod = diffPerMethod;
        assertNotNull(diffPerMethod);
    }

    @Order(1)
    @Test
    void firstConnection(@ContainerRedisConnection RedisConnection connection) {
        assertNull(firstConnection);
        assertNotNull(connection);
        assertNotNull(connection.params().uri());
        firstConnection = connection;
        assertNotNull(diffPerMethod);
        assertEquals(diffPerMethod, connection);
    }

    @Order(2)
    @Test
    void secondConnection(@ContainerRedisConnection RedisConnection connection) {
        assertNotNull(connection);
        assertNotNull(connection.params().uri());
        assertNotNull(diffPerMethod);
        assertEquals(diffPerMethod, connection);

        assertNotNull(firstConnection);
        assertNotEquals(firstConnection, connection);
    }
}
