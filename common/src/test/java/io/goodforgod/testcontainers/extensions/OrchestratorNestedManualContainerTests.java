package io.goodforgod.testcontainers.extensions;

import static org.junit.jupiter.api.Assertions.*;

import io.goodforgod.testcontainers.extensions.orchestrator.FakeConnection;
import io.goodforgod.testcontainers.extensions.orchestrator.FakeGenericContainer;
import io.goodforgod.testcontainers.extensions.orchestrator.redis.ConnectionRedis;
import io.goodforgod.testcontainers.extensions.orchestrator.redis.ContainerRedis;
import io.goodforgod.testcontainers.extensions.orchestrator.redis.TestcontainersRedis;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@TestcontainersRedis(mode = ContainerMode.PER_METHOD)
class OrchestratorNestedManualContainerTests {

    @ContainerRedis
    private final FakeGenericContainer redis = new FakeGenericContainer("redis:nested")
            .withNetworkAliases("redis-nested");

    @Nested
    class NestedTests {

        @ConnectionRedis
        private FakeConnection connection;

        @Test
        void usesParentManualContainer() {
            assertEquals("redis:nested", connection.image());
            assertTrue(connection.aliases().contains("redis-nested"));
        }
    }
}
