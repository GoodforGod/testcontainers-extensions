package io.goodforgod.testcontainers.extensions.redis;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.goodforgod.testcontainers.extensions.ContainerMode;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.utility.DockerImageName;

@TestcontainersRedis(mode = ContainerMode.PER_METHOD, image = "redis:7.0-alpine")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ContainerFromAnnotationTests {

    @ContainerRedis
    private static final RedisContainer container = new RedisContainer(DockerImageName.parse("redis:7.0-alpine"))
            .withNetworkAliases("myredis")
            .withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger(RedisContainer.class)));

    @Test
    void checkParams(@ContainerRedisConnection RedisConnection connection) {
        assertEquals("myredis", connection.paramsInNetwork().get().host());
    }

    @Test
    void checkParamsAgain(@ContainerRedisConnection RedisConnection connection) {
        assertEquals("myredis", connection.paramsInNetwork().get().host());
    }
}
