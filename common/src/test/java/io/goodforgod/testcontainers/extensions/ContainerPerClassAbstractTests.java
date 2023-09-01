package io.goodforgod.testcontainers.extensions;

import static org.junit.jupiter.api.Assertions.*;

import io.goodforgod.testcontainers.extensions.example.ContainerRedisConnection;
import io.goodforgod.testcontainers.extensions.example.RedisConnection;
import io.goodforgod.testcontainers.extensions.example.TestcontainersRedis;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@TestcontainersRedis(mode = ContainerMode.PER_CLASS, image = "redis:7.2-alpine")
abstract class ContainerPerClassAbstractTests {

    @ContainerRedisConnection
    protected RedisConnection sameConnectionParent;

    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    public static class TestClassChild extends ContainerPerClassAbstractTests {

        private final RedisConnection sameConnectionChild;

        private static RedisConnection firstConnection;

        TestClassChild(@ContainerRedisConnection RedisConnection sameConnectionChild) {
            this.sameConnectionChild = sameConnectionChild;
            assertNotNull(sameConnectionChild);
        }

        @Order(1)
        @Test
        void firstConnection(@ContainerRedisConnection RedisConnection connection) {
            assertNull(firstConnection);
            assertNotNull(connection);
            assertNotNull(connection.params().uri());
            assertTrue(connection.paramsInNetwork().isPresent());
            assertNotNull(connection.paramsInNetwork().get().uri());
            assertNotNull(connection.paramsInNetwork().get().host());
            assertFalse(connection.paramsInNetwork().get().host().isBlank());
            assertNotNull(sameConnectionChild);
            assertEquals(sameConnectionChild, connection);
            assertEquals(sameConnectionChild, sameConnectionParent);
            firstConnection = connection;
        }

        @Order(2)
        @Test
        void secondConnection(@ContainerRedisConnection RedisConnection connection) {
            assertNotNull(connection);
            assertNotNull(connection.params().uri());
            assertTrue(connection.paramsInNetwork().isPresent());
            assertNotNull(connection.paramsInNetwork().get().uri());
            assertNotNull(connection.paramsInNetwork().get().host());
            assertFalse(connection.paramsInNetwork().get().host().isBlank());
            assertNotNull(firstConnection);
            assertNotNull(sameConnectionChild);
            assertEquals(sameConnectionChild, connection);
            assertEquals(sameConnectionChild, sameConnectionParent);
            assertEquals(firstConnection, connection);
        }
    }
}
