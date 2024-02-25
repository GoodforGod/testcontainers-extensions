package io.goodforgod.testcontainers.extensions;

import static org.junit.jupiter.api.Assertions.*;

import io.goodforgod.testcontainers.extensions.example.ConnectionRedis;
import io.goodforgod.testcontainers.extensions.example.RedisConnection;
import io.goodforgod.testcontainers.extensions.example.TestcontainersRedis;
import org.junit.jupiter.api.*;

@TestcontainersRedis(mode = ContainerMode.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ContainerPerClassNestedTests {

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
            assertSame(samePerMethodConnection, connection);
        }

        @Order(2)
        @Test
        void secondConnection(@ConnectionRedis RedisConnection connection) {
            assertNotNull(connection);
            assertNotNull(connection.params().uri());
            assertNotNull(samePerMethodConnection);
            assertEquals(samePerMethodConnection, connection);
            assertSame(samePerMethodConnection, connection);
            assertNotNull(firstConnection);
            assertEquals(firstConnection, connection);
            assertSame(firstConnection, connection);

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
            assertSame(samePerMethodConnection, connection);
        }

        @Order(2)
        @Test
        void secondConnection(@ConnectionRedis RedisConnection connection) {
            assertNotNull(connection);
            assertNotNull(connection.params().uri());
            assertNotNull(samePerMethodConnection);
            assertEquals(samePerMethodConnection, connection);
            assertSame(samePerMethodConnection, connection);
            assertNotNull(firstConnection);
            assertEquals(firstConnection, connection);
            assertSame(firstConnection, connection);

            nextNested = true;
        }
    }
}
