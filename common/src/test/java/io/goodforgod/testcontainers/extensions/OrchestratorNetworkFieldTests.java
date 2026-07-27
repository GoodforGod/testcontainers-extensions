package io.goodforgod.testcontainers.extensions;

import static org.junit.jupiter.api.Assertions.*;

import io.goodforgod.testcontainers.extensions.orchestrator.FakeConnection;
import io.goodforgod.testcontainers.extensions.orchestrator.minio.ConnectionMinio;
import io.goodforgod.testcontainers.extensions.orchestrator.minio.TestcontainersMinio;
import io.goodforgod.testcontainers.extensions.orchestrator.redis.ConnectionRedis;
import io.goodforgod.testcontainers.extensions.orchestrator.redis.TestcontainersRedis;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@TestcontainersRedis(mode = ContainerMode.PER_CLASS)
@TestcontainersMinio(mode = ContainerMode.PER_CLASS)
class OrchestratorNetworkFieldTests {

    @ContainerNetwork
    private static final org.testcontainers.containers.Network NETWORK = org.testcontainers.containers.Network.newNetwork();

    @ConnectionRedis
    private FakeConnection redis;

    @ConnectionMinio
    private FakeConnection minio;

    @Test
    void usesNetworkFromAnnotatedFieldForAllContainers() {
        int expectedNetwork = System.identityHashCode(NETWORK);
        assertEquals(expectedNetwork, redis.networkIdentity());
        assertEquals(expectedNetwork, minio.networkIdentity());
    }

    @Nested
    @TestcontainersRedis(mode = ContainerMode.PER_METHOD)
    class NestedTests {

        @ConnectionRedis
        private FakeConnection nestedRedis;

        @Test
        void usesParentNetworkField() {
            assertEquals(System.identityHashCode(NETWORK), nestedRedis.networkIdentity());
        }
    }
}
