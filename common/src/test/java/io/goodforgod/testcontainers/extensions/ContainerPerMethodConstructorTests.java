package io.goodforgod.testcontainers.extensions;

import static org.junit.jupiter.api.Assertions.*;

import io.goodforgod.testcontainers.extensions.example.ConnectionRedis;
import io.goodforgod.testcontainers.extensions.example.RedisConnection;
import io.goodforgod.testcontainers.extensions.example.TestcontainersRedis;
import org.junit.jupiter.api.*;

@TestcontainersRedis(mode = ContainerMode.PER_METHOD)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class ContainerPerMethodConstructorTests {

    private final RedisConnection diffPerMethod;

    private static RedisConnection firstConnection;

    ContainerPerMethodConstructorTests(@ConnectionRedis RedisConnection diffPerMethod) {
        this.diffPerMethod = diffPerMethod;
        assertNotNull(diffPerMethod);
    }

    @Order(1)
    @Test
    void firstConnection(@ConnectionRedis RedisConnection connection) {
        assertNull(firstConnection);
        assertNotNull(connection);
        assertNotNull(connection.params().uri());
        firstConnection = connection;
        assertNotNull(diffPerMethod);
        assertEquals(diffPerMethod, connection);
    }

    @Order(2)
    @Test
    void secondConnection(@ConnectionRedis RedisConnection connection) {
        assertNotNull(connection);
        assertNotNull(connection.params().uri());
        assertNotNull(diffPerMethod);
        assertEquals(diffPerMethod, connection);

        assertNotNull(firstConnection);
        assertNotEquals(firstConnection, connection);
    }
}
