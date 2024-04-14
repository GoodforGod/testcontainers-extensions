package io.goodforgod.testcontainers.extensions;

import static org.junit.jupiter.api.Assertions.*;

import io.goodforgod.testcontainers.extensions.example.ConnectionRedis;
import io.goodforgod.testcontainers.extensions.example.RedisConnection;
import io.goodforgod.testcontainers.extensions.example.TestcontainersRedis;
import org.junit.jupiter.api.*;

@TestcontainersRedis(mode = ContainerMode.PER_METHOD)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ContainerPerMethodNestedTests {

    @ConnectionRedis
    private RedisConnection samePerMethodConnection;

    private static RedisConnection firstConnection;

    private static boolean nextNested = false;

    @BeforeEach
    void setupNested() {
        if (nextNested) {
            firstConnection = null;
            nextNested = false;
        }
    }

    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    @Nested
    public class NestedFirstTests {

        @Order(1)
        @Test
        void firstConnection(@ConnectionRedis RedisConnection connection) {
            assertNull(firstConnection);
            assertNotNull(connection);
            assertNotNull(connection.params().uri());
            firstConnection = connection;
            assertNotNull(samePerMethodConnection);
            assertEquals(samePerMethodConnection, connection);
        }

        @Order(2)
        @Test
        void secondConnection(@ConnectionRedis RedisConnection connection) {
            assertNotNull(connection);
            assertNotNull(connection.params().uri());
            assertNotNull(samePerMethodConnection);
            assertEquals(samePerMethodConnection, connection);
            assertNotNull(firstConnection);
            assertNotEquals(firstConnection, connection);

            nextNested = true;
        }
    }

    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    @Nested
    public class NestedSecondTests {

        @Order(1)
        @Test
        void firstConnection(@ConnectionRedis RedisConnection connection) {
            assertNull(firstConnection);
            assertNotNull(connection);
            assertNotNull(connection.params().uri());
            firstConnection = connection;
            assertNotNull(samePerMethodConnection);
            assertEquals(samePerMethodConnection, connection);
        }

        @Order(2)
        @Test
        void secondConnection(@ConnectionRedis RedisConnection connection) {
            assertNotNull(connection);
            assertNotNull(connection.params().uri());
            assertNotNull(samePerMethodConnection);
            assertEquals(samePerMethodConnection, connection);
            assertNotNull(firstConnection);
            assertNotEquals(firstConnection, connection);

            nextNested = true;
        }
    }
}
