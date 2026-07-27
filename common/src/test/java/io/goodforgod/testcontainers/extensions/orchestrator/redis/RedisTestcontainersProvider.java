package io.goodforgod.testcontainers.extensions.orchestrator.redis;

import io.goodforgod.testcontainers.extensions.*;
import io.goodforgod.testcontainers.extensions.orchestrator.FakeConnection;
import io.goodforgod.testcontainers.extensions.orchestrator.FakeContainerContext;
import io.goodforgod.testcontainers.extensions.orchestrator.FakeGenericContainer;
import io.goodforgod.testcontainers.extensions.orchestrator.StartTimeline;
import java.lang.annotation.Annotation;
import org.jetbrains.annotations.NotNull;
import org.testcontainers.containers.GenericContainer;

public final class RedisTestcontainersProvider implements TestcontainersProvider<TestcontainersRedis, FakeConnection> {

    @Override
    public @NotNull Class<TestcontainersRedis> annotationType() {
        return TestcontainersRedis.class;
    }

    @Override
    public @NotNull Class<? extends Annotation> containerAnnotationType() {
        return ContainerRedis.class;
    }

    @Override
    public @NotNull Class<? extends Annotation> connectionAnnotationType() {
        return ConnectionRedis.class;
    }

    @Override
    public @NotNull Class<FakeConnection> connectionType() {
        return FakeConnection.class;
    }

    @Override
    public @NotNull ContainerMode mode(@NotNull TestcontainersRedis annotation) {
        return annotation.mode();
    }

    @Override
    public @NotNull String image(@NotNull TestcontainersRedis annotation) {
        return annotation.image();
    }

    @Override
    public boolean networkShared(@NotNull TestcontainersRedis annotation) {
        return annotation.network().shared();
    }

    @Override
    public String networkAlias(@NotNull TestcontainersRedis annotation) {
        return annotation.network().alias();
    }

    @Override
    public Isolation.Mode isolation(@NotNull TestcontainersRedis annotation) {
        return annotation.isolation().value();
    }

    @Override
    public String isolationPrefix(@NotNull TestcontainersRedis annotation) {
        return "redis";
    }

    @Override
    public @NotNull GenericContainer<?> createContainer(@NotNull TestcontainersRedis annotation) {
        return new FakeGenericContainer(annotation.image());
    }

    @Override
    public @NotNull ContainerContext<FakeConnection> createContext(@NotNull GenericContainer<?> container) {
        return new FakeContainerContext("redis", container.getDockerImageName(), container);
    }

    @Override
    public @NotNull FakeConnection createIsolatedConnection(@NotNull TestcontainersRedis annotation,
                                                            @NotNull ContainerContext<FakeConnection> context,
                                                            @NotNull org.junit.jupiter.api.extension.ExtensionContext extension,
                                                            @NotNull String namespace) {
        FakeConnection base = context.connection();
        return new FakeConnection(base.service(),
                base.image(),
                base.aliases(),
                base.sharedNetwork(),
                base.networkIdentity(),
                namespace);
    }

    @Override
    public void beforeEach(@NotNull TestcontainersRedis annotation,
                           @NotNull ContainerContext<FakeConnection> context,
                           @NotNull org.junit.jupiter.api.extension.ExtensionContext extension) {
        hook("redis", "beforeEach");
    }

    @Override
    public void afterEach(@NotNull TestcontainersRedis annotation,
                          @NotNull ContainerContext<FakeConnection> context,
                          @NotNull org.junit.jupiter.api.extension.ExtensionContext extension) {
        hook("redis", "afterEach");
    }

    private static void hook(String service, String hook) {
        long startedAt = System.nanoTime();
        try {
            Thread.sleep(250);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }

        StartTimeline.hooked(service, hook, startedAt, System.nanoTime());
    }
}
