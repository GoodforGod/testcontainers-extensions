package io.goodforgod.testcontainers.extensions.redis;

import io.goodforgod.testcontainers.extensions.ContainerContext;
import io.goodforgod.testcontainers.extensions.ContainerMode;
import io.goodforgod.testcontainers.extensions.TestcontainersProvider;
import java.lang.annotation.Annotation;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.testcontainers.containers.GenericContainer;

@Internal
public final class RedisTestcontainersProvider implements TestcontainersProvider<TestcontainersRedis, RedisConnection> {

    private final TestcontainersRedisExtension delegate = new TestcontainersRedisExtension();

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
    public @NotNull Class<RedisConnection> connectionType() {
        return RedisConnection.class;
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
    public @NotNull GenericContainer<?> createContainer(@NotNull TestcontainersRedis annotation) {
        var metadata = new RedisMetadata(annotation.network().shared(), annotation.network().alias(), annotation.image(),
                annotation.mode());
        return delegate.createContainerDefault(metadata);
    }

    @Override
    public @NotNull ContainerContext<RedisConnection> createContext(@NotNull GenericContainer<?> container) {
        return delegate.createContainerContext((RedisContainer) container);
    }
}
