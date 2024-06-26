package io.goodforgod.testcontainers.extensions;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.goodforgod.testcontainers.extensions.example.*;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.output.Slf4jLogConsumer;

@TestcontainersRedis(mode = ContainerMode.PER_METHOD)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ContainerFromAnnotationTests {

    @ContainerRedis
    private static final RedisContainer container = new RedisContainer("redis:7.2-alpine")
            .withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger(RedisContainer.class)));

    @Test
    void checkParams(@ConnectionRedis RedisConnection connection) {
        assertTrue(container.isRunning());
        connection.deleteAll();
    }

    @Test
    void checkParamsAgain(@ConnectionRedis RedisConnection connection) {
        assertTrue(container.isRunning());
        connection.deleteAll();
    }
}
