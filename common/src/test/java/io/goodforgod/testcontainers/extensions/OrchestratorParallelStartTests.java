package io.goodforgod.testcontainers.extensions;

import static org.junit.jupiter.api.Assertions.*;

import io.goodforgod.testcontainers.extensions.orchestrator.FakeConnection;
import io.goodforgod.testcontainers.extensions.orchestrator.StartTimeline;
import io.goodforgod.testcontainers.extensions.orchestrator.minio.ConnectionMinio;
import io.goodforgod.testcontainers.extensions.orchestrator.minio.TestcontainersMinio;
import io.goodforgod.testcontainers.extensions.orchestrator.redis.ConnectionRedis;
import io.goodforgod.testcontainers.extensions.orchestrator.redis.TestcontainersRedis;
import org.junit.jupiter.api.Test;

@TestcontainersRedis(mode = ContainerMode.PER_CLASS)
@TestcontainersMinio(mode = ContainerMode.PER_CLASS)
class OrchestratorParallelStartTests {

    @ConnectionRedis
    private FakeConnection redis;

    @ConnectionMinio
    private FakeConnection minio;

    @Test
    void startsAnnotatedServicesInParallel(@ConnectionRedis FakeConnection redisParam,
                                           @ConnectionMinio FakeConnection minioParam) {
        assertEquals("redis", redis.service());
        assertEquals("minio", minio.service());
        assertEquals(redis, redisParam);
        assertEquals(minio, minioParam);

        var redisStart = StartTimeline.event("redis");
        var minioStart = StartTimeline.event("minio");
        assertNotNull(redisStart);
        assertNotNull(minioStart);
        assertTrue(redisStart.start() < minioStart.end() && minioStart.start() < redisStart.end(),
                "Expected Redis and Minio start intervals to overlap");

        var redisBeforeEach = StartTimeline.event("beforeEach:redis");
        var minioBeforeEach = StartTimeline.event("beforeEach:minio");
        assertNotNull(redisBeforeEach);
        assertNotNull(minioBeforeEach);
        assertTrue(redisBeforeEach.start() < minioBeforeEach.end() && minioBeforeEach.start() < redisBeforeEach.end(),
                "Expected Redis and Minio beforeEach intervals to overlap");
    }
}
