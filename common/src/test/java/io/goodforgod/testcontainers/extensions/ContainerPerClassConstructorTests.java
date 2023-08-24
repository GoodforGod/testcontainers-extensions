package io.goodforgod.testcontainers.extensions;

import static org.junit.jupiter.api.Assertions.*;

import io.goodforgod.testcontainers.extensions.example.ContainerRedisConnection;
import io.goodforgod.testcontainers.extensions.example.RedisConnection;
import io.goodforgod.testcontainers.extensions.example.TestcontainersRedis;
import org.junit.jupiter.api.*;

@TestcontainersRedis(mode = ContainerMode.PER_CLASS,
        image = "redis:7.2-alpine",
        network = @Network(shared = false, alias = "my_alias"))
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ContainerPerClassConstructorTests {

    private final RedisConnection sameConnection;

    private static RedisConnection firstConnection;

    ContainerPerClassConstructorTests(@ContainerRedisConnection RedisConnection sameConnection) {
        this.sameConnection = sameConnection;
        assertNotNull(sameConnection);
    }

    @Order(1)
    @Test
    void firstConnection(@ContainerRedisConnection RedisConnection connection) {
        assertEquals("my_alias", connection.paramsInNetwork().orElseThrow().host());
        assertNull(firstConnection);
        assertNotNull(connection);
        assertNotNull(connection.params().uri());
        assertNotNull(sameConnection);
        assertEquals(sameConnection, connection);
        firstConnection = connection;
    }

    @Order(2)
    @Test
    void secondConnection(@ContainerRedisConnection RedisConnection connection) {
        assertEquals("my_alias", connection.paramsInNetwork().orElseThrow().host());
        assertNotNull(connection);
        assertNotNull(connection.params().uri());
        assertNotNull(firstConnection);
        assertNotNull(sameConnection);
        assertEquals(sameConnection, connection);
        assertEquals(firstConnection, connection);
    }
}
