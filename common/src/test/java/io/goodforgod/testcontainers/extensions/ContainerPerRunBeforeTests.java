package io.goodforgod.testcontainers.extensions;

import static org.junit.jupiter.api.Assertions.*;

import io.goodforgod.testcontainers.extensions.example.ContainerRedisConnection;
import io.goodforgod.testcontainers.extensions.example.RedisConnection;
import io.goodforgod.testcontainers.extensions.example.TestcontainersRedis;
import org.junit.jupiter.api.*;

@TestcontainersRedis(mode = ContainerMode.PER_RUN, image = "redis:7.2.0-alpine")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ContainerPerRunBeforeTests {

    private static RedisConnection setupAll;
    private static RedisConnection setupEach;
    private static RedisConnection setupFirst;

    @ContainerRedisConnection
    private RedisConnection fieldConnection;

    @BeforeAll
    public static void setupAll(@ContainerRedisConnection RedisConnection paramConnection) {
        assertNotNull(paramConnection);
        assertNotNull(paramConnection.params().uri());
        setupAll = paramConnection;
    }

    @BeforeEach
    public void setupEach(@ContainerRedisConnection RedisConnection paramConnection) {
        assertEquals(fieldConnection, paramConnection);
        setupEach = paramConnection;
    }

    @Order(1)
    @Test
    void firstConnection(@ContainerRedisConnection RedisConnection paramConnection) {
        assertNotNull(paramConnection);
        assertNotNull(paramConnection.params().uri());
        assertNotNull(fieldConnection);
        assertEquals(fieldConnection, paramConnection);
        assertEquals(setupAll, paramConnection);
        assertEquals(setupEach, paramConnection);
        setupFirst = paramConnection;
    }

    @Order(2)
    @Test
    void secondConnection(@ContainerRedisConnection RedisConnection paramConnection) {
        assertNotNull(paramConnection);
        assertNotNull(paramConnection.params().uri());
        assertNotNull(fieldConnection);
        assertEquals(fieldConnection, paramConnection);
        assertEquals(setupAll, paramConnection);
        assertEquals(setupEach, paramConnection);
        assertEquals(setupFirst, paramConnection);
    }
}
