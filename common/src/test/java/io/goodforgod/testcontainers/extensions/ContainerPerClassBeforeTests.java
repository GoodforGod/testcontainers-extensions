package io.goodforgod.testcontainers.extensions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.goodforgod.testcontainers.extensions.example.ConnectionRedis;
import io.goodforgod.testcontainers.extensions.example.RedisConnection;
import io.goodforgod.testcontainers.extensions.example.TestcontainersRedis;
import org.junit.jupiter.api.*;

@TestcontainersRedis(mode = ContainerMode.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ContainerPerClassBeforeTests {

    private static RedisConnection setupAll;
    private static RedisConnection setupEach;
    private static RedisConnection setupFirst;

    @ConnectionRedis
    private RedisConnection fieldConnection;

    @BeforeAll
    public static void setupAll(@ConnectionRedis RedisConnection paramConnection) {
        assertNotNull(paramConnection);
        assertNotNull(paramConnection.params().uri());
        setupAll = paramConnection;
    }

    @BeforeEach
    public void setupEach(@ConnectionRedis RedisConnection paramConnection) {
        assertEquals(fieldConnection, paramConnection);
        setupEach = paramConnection;
    }

    @Order(1)
    @Test
    void firstConnection(@ConnectionRedis RedisConnection paramConnection) {
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
    void secondConnection(@ConnectionRedis RedisConnection paramConnection) {
        assertNotNull(paramConnection);
        assertNotNull(paramConnection.params().uri());
        assertNotNull(fieldConnection);
        assertEquals(fieldConnection, paramConnection);
        assertEquals(setupAll, paramConnection);
        assertEquals(setupEach, paramConnection);
        assertEquals(setupFirst, paramConnection);
    }
}
