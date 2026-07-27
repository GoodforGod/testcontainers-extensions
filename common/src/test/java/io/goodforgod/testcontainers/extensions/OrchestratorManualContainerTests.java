package io.goodforgod.testcontainers.extensions;

import static org.junit.jupiter.api.Assertions.*;

import io.goodforgod.testcontainers.extensions.orchestrator.FakeConnection;
import io.goodforgod.testcontainers.extensions.orchestrator.FakeGenericContainer;
import io.goodforgod.testcontainers.extensions.orchestrator.redis.ConnectionRedis;
import io.goodforgod.testcontainers.extensions.orchestrator.redis.ContainerRedis;
import io.goodforgod.testcontainers.extensions.orchestrator.redis.TestcontainersRedis;
import org.junit.jupiter.api.Test;

@TestcontainersRedis(mode = ContainerMode.PER_CLASS)
class OrchestratorManualContainerTests {

    @ContainerRedis
    private static final FakeGenericContainer REDIS = new FakeGenericContainer("redis:manual")
            .withNetworkAliases("redis-manual");

    @ConnectionRedis
    private FakeConnection redis;

    @Test
    void usesStaticManualContainer() {
        assertEquals("redis:manual", redis.image());
        assertTrue(redis.aliases().contains("redis-manual"));
    }
}
