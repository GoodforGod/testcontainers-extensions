package io.goodforgod.testcontainers.extensions;

import static org.junit.jupiter.api.Assertions.*;

import io.goodforgod.testcontainers.extensions.orchestrator.FakeConnection;
import io.goodforgod.testcontainers.extensions.orchestrator.redis.ConnectionRedis;
import io.goodforgod.testcontainers.extensions.orchestrator.redis.TestcontainersRedis;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Execution(ExecutionMode.CONCURRENT)
@TestcontainersRedis(mode = ContainerMode.PER_RUN, isolation = @Isolation(Isolation.Mode.PER_METHOD))
class OrchestratorIsolationPerMethodTests {

    private static final Set<String> NAMESPACES = ConcurrentHashMap.newKeySet();

    @ConnectionRedis
    private FakeConnection redis;

    @Test
    void injectsIsolatedFieldConnection(@ConnectionRedis FakeConnection redisParam) {
        assertSame(redis, redisParam);
        assertTrue(redis.namespace().startsWith("redis_"));
        assertTrue(NAMESPACES.add(redis.namespace()));
    }

    @Test
    void injectsDifferentIsolatedFieldConnection(@ConnectionRedis FakeConnection redisParam) {
        assertSame(redis, redisParam);
        assertTrue(redis.namespace().startsWith("redis_"));
        assertTrue(NAMESPACES.add(redis.namespace()));
    }
}
