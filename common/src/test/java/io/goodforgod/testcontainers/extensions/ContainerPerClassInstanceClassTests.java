package io.goodforgod.testcontainers.extensions;

import io.goodforgod.testcontainers.extensions.example.ContainerRedisConnection;
import io.goodforgod.testcontainers.extensions.example.RedisConnection;
import io.goodforgod.testcontainers.extensions.example.TestcontainersRedis;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

@TestcontainersRedis(mode = ContainerMode.PER_CLASS,
        image = "${MY_IMAGE_ENV}",
        network = @Network(shared = false, alias = "${MY_ALIAS_ENV}"))
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ContainerPerClassInstanceClassTests {

    @ContainerRedisConnection
    private RedisConnection sameConnection;

    private static RedisConnection firstConnection;

    @Order(1)
    @Test
    void firstConnection(@ContainerRedisConnection RedisConnection connection) {
        assertEquals("my_alias_env", connection.paramsInNetwork().orElseThrow().host());
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
        assertEquals("my_alias_env", connection.paramsInNetwork().orElseThrow().host());
        assertNotNull(connection);
        assertNotNull(connection.params().uri());
        assertNotNull(firstConnection);
        assertNotNull(sameConnection);
        assertEquals(sameConnection, connection);
        assertEquals(firstConnection, connection);
    }
}
