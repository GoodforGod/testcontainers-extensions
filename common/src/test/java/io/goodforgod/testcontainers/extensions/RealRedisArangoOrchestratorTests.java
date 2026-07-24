package io.goodforgod.testcontainers.extensions;

import static org.junit.jupiter.api.Assertions.*;

import io.goodforgod.testcontainers.extensions.arangodb.ArangoConnection;
import io.goodforgod.testcontainers.extensions.arangodb.ConnectionArango;
import io.goodforgod.testcontainers.extensions.arangodb.TestcontainersArango;
import io.goodforgod.testcontainers.extensions.redis.ConnectionRedis;
import io.goodforgod.testcontainers.extensions.redis.RedisConnection;
import io.goodforgod.testcontainers.extensions.redis.RedisKey;
import io.goodforgod.testcontainers.extensions.redis.TestcontainersRedis;
import io.testcontainers.arangodb.containers.ArangoContainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@TestcontainersRedis(mode = ContainerMode.PER_CLASS, image = "redis:7.0-alpine")
@TestcontainersArango(mode = ContainerMode.PER_CLASS, image = "arangodb:3.12.4")
class RealRedisArangoOrchestratorTests {

    @ConnectionRedis
    private RedisConnection redis;

    @ConnectionArango
    private ArangoConnection arango;

    @BeforeEach
    void cleanRedis() {
        redis.deleteAll();
    }

    @Test
    void injectsBothRealModuleConnections(@ConnectionRedis RedisConnection redisParam,
                                          @ConnectionArango ArangoConnection arangoParam) {
        assertSame(redis, redisParam);
        assertSame(arango, arangoParam);

        redis.getConnection().set("real-module-key", "value");
        assertEquals(1, redis.count(RedisKey.of("real-module-key")));

        assertNotNull(arango.client());
        assertEquals("root", arango.params().username());
        assertTrue(arango.params().port() > 0);
        assertEquals(ArangoContainer.PORT, arango.paramsInNetwork().orElseThrow().port());
    }
}
