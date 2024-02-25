package io.goodforgod.testcontainers.extensions;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertSame;

import io.goodforgod.testcontainers.extensions.example.ConnectionRedis;
import io.goodforgod.testcontainers.extensions.example.RedisConnection;
import io.goodforgod.testcontainers.extensions.example.TestcontainersRedis;
import org.junit.jupiter.api.*;

@TestcontainersRedis(mode = ContainerMode.PER_RUN)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ContainerPerRunNestedFirstTests {

    static volatile RedisConnection perRunConnection;

    @ConnectionRedis
    private RedisConnection sameConnection;

    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    @Nested
    public class NestedFirstTests {

        @Order(1)
        @Test
        void firstConnection(@ConnectionRedis RedisConnection connection) {
            assertNotNull(connection);
            assertNotNull(connection.params().uri());
            assertNotNull(sameConnection);
            assertEquals(sameConnection, connection);

            if (perRunConnection == null) {
                perRunConnection = connection;
            }

            if (ContainerPerRunSecondTests.perRunConnection != null) {
                assertEquals(perRunConnection, ContainerPerRunSecondTests.perRunConnection);
            }
        }

        @Order(2)
        @Test
        void secondConnection(@ConnectionRedis RedisConnection connection) {
            assertNotNull(connection);
            assertNotNull(connection.params().uri());
            assertNotNull(sameConnection);
            assertEquals(sameConnection, connection);
            assertEquals(perRunConnection, connection);
            assertSame(sameConnection, connection);
            assertSame(perRunConnection, connection);
        }
    }

    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    @Nested
    public class NestedSecondTests {

        @Order(1)
        @Test
        void firstConnection(@ConnectionRedis RedisConnection connection) {
            assertNotNull(connection);
            assertNotNull(connection.params().uri());
            assertNotNull(sameConnection);
            assertEquals(sameConnection, connection);
            assertSame(sameConnection, connection);

            if (perRunConnection == null) {
                perRunConnection = connection;
            }

            if (ContainerPerRunSecondTests.perRunConnection != null) {
                assertEquals(perRunConnection, ContainerPerRunSecondTests.perRunConnection);
            }
        }

        @Order(2)
        @Test
        void secondConnection(@ConnectionRedis RedisConnection connection) {
            assertNotNull(connection);
            assertNotNull(connection.params().uri());
            assertNotNull(sameConnection);
            assertEquals(sameConnection, connection);
            assertEquals(perRunConnection, connection);
            assertSame(sameConnection, connection);
            assertSame(perRunConnection, connection);
        }
    }
}
