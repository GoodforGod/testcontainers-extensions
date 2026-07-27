package io.goodforgod.testcontainers.extensions;

import static org.junit.jupiter.api.Assertions.*;

import io.goodforgod.testcontainers.extensions.orchestrator.StartTimeline;
import io.goodforgod.testcontainers.extensions.orchestrator.minio.TestcontainersMinio;
import io.goodforgod.testcontainers.extensions.orchestrator.redis.TestcontainersRedis;
import org.junit.jupiter.api.Test;

@TestcontainersRedis(mode = ContainerMode.PER_CLASS)
@TestcontainersMinio(mode = ContainerMode.PER_CLASS, dependsOnRedis = true)
class OrchestratorDependencyStartTests {

    @Test
    void startsDependentProviderAfterDependency() {
        var redisStart = StartTimeline.event("redis");
        var minioStart = StartTimeline.event("minio");
        assertNotNull(redisStart);
        assertNotNull(minioStart);
        assertTrue(redisStart.end() <= minioStart.start(), "Expected Minio to start after Redis");

        var redisBeforeEach = StartTimeline.event("beforeEach:redis");
        var minioBeforeEach = StartTimeline.event("beforeEach:minio");
        assertNotNull(redisBeforeEach);
        assertNotNull(minioBeforeEach);
        assertTrue(redisBeforeEach.end() <= minioBeforeEach.start(), "Expected Minio beforeEach after Redis");
    }
}
