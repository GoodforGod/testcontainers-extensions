package io.goodforgod.testcontainers.extensions;

import io.goodforgod.testcontainers.extensions.example.ContainerRedisConnection;
import io.goodforgod.testcontainers.extensions.example.RedisConnection;
import io.goodforgod.testcontainers.extensions.example.TestcontainersRedis;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

@TestcontainersRedis(mode = ContainerMode.PER_CLASS,
        image = "${MY_IMAGE_ENV_EMPTY|redis:7.2-alpine}",
        network = @Network(shared = false, alias = "${MY_ALIAS_ENV_EMPTY|my_default_alias}"))
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class ContainerPerClassInstanceMethodTests {

    @ContainerRedisConnection
    private RedisConnection sameConnection;

    private static RedisConnection firstConnection;

    @Order(1)
    @Test
    void firstConnection(@ContainerRedisConnection RedisConnection connection) {
        assertEquals("my_default_alias", connection.paramsInNetwork().orElseThrow().host());
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
        assertEquals("my_default_alias", connection.paramsInNetwork().orElseThrow().host());
        assertNotNull(connection);
        assertNotNull(connection.params().uri());
        assertNotNull(firstConnection);
        assertNotNull(sameConnection);
        assertEquals(sameConnection, connection);
        assertEquals(firstConnection, connection);
    }
}
