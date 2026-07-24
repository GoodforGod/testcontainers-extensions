package io.goodforgod.testcontainers.extensions.orchestrator.minio;

import io.goodforgod.testcontainers.extensions.*;
import io.goodforgod.testcontainers.extensions.orchestrator.FakeConnection;
import io.goodforgod.testcontainers.extensions.orchestrator.FakeContainerContext;
import io.goodforgod.testcontainers.extensions.orchestrator.FakeGenericContainer;
import io.goodforgod.testcontainers.extensions.orchestrator.StartTimeline;
import io.goodforgod.testcontainers.extensions.orchestrator.redis.TestcontainersRedis;
import java.lang.annotation.Annotation;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.testcontainers.containers.GenericContainer;

public final class MinioTestcontainersProvider implements TestcontainersProvider<TestcontainersMinio, FakeConnection> {

    @Override
    public @NotNull Class<TestcontainersMinio> annotationType() {
        return TestcontainersMinio.class;
    }

    @Override
    public @NotNull Class<? extends Annotation> containerAnnotationType() {
        return ContainerMinio.class;
    }

    @Override
    public @NotNull Class<? extends Annotation> connectionAnnotationType() {
        return ConnectionMinio.class;
    }

    @Override
    public @NotNull Class<FakeConnection> connectionType() {
        return FakeConnection.class;
    }

    @Override
    public @NotNull ContainerMode mode(@NotNull TestcontainersMinio annotation) {
        return annotation.mode();
    }

    @Override
    public @NotNull String image(@NotNull TestcontainersMinio annotation) {
        return annotation.image();
    }

    @Override
    public boolean networkShared(@NotNull TestcontainersMinio annotation) {
        return annotation.network().shared();
    }

    @Override
    public String networkAlias(@NotNull TestcontainersMinio annotation) {
        return annotation.network().alias();
    }

    @Override
    public Set<Class<? extends Annotation>> dependencies(@NotNull TestcontainersMinio annotation) {
        return annotation.dependsOnRedis()
                ? Set.of(TestcontainersRedis.class)
                : Set.of();
    }

    @Override
    public @NotNull GenericContainer<?> createContainer(@NotNull TestcontainersMinio annotation) {
        return new FakeGenericContainer(annotation.image());
    }

    @Override
    public @NotNull ContainerContext<FakeConnection> createContext(@NotNull GenericContainer<?> container) {
        return new FakeContainerContext("minio", container.getDockerImageName(), container);
    }

    @Override
    public void beforeEach(@NotNull TestcontainersMinio annotation,
                           @NotNull ContainerContext<FakeConnection> context,
                           @NotNull org.junit.jupiter.api.extension.ExtensionContext extension) {
        hook("minio", "beforeEach");
    }

    @Override
    public void afterEach(@NotNull TestcontainersMinio annotation,
                          @NotNull ContainerContext<FakeConnection> context,
                          @NotNull org.junit.jupiter.api.extension.ExtensionContext extension) {
        hook("minio", "afterEach");
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
