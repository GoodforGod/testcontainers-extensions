package io.goodforgod.testcontainers.extensions;

import static org.junit.jupiter.api.Assertions.*;

import io.goodforgod.testcontainers.extensions.orchestrator.FakeConnection;
import io.goodforgod.testcontainers.extensions.orchestrator.redis.ConnectionRedis;
import io.goodforgod.testcontainers.extensions.orchestrator.redis.TestcontainersRedis;
import org.junit.jupiter.api.Test;

@TestcontainersRedis(mode = ContainerMode.PER_CLASS,
        network = @Network(shared = true, alias = "redis-shared"))
class OrchestratorSharedNetworkTests {

    @ConnectionRedis
    private FakeConnection redis;

    @Test
    void appliesSharedNetworkAndAlias() {
        assertTrue(redis.sharedNetwork());
        assertTrue(redis.aliases().contains("redis-shared"));
    }
}
