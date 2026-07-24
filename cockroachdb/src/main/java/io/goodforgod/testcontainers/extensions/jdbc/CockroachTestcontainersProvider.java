package io.goodforgod.testcontainers.extensions.jdbc;

import io.goodforgod.testcontainers.extensions.Isolation;
import java.lang.annotation.Annotation;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.testcontainers.containers.CockroachContainer;

@Internal
public final class CockroachTestcontainersProvider extends
        AbstractJdbcTestcontainersProvider<TestcontainersCockroach, CockroachContainer> {

    private final TestcontainersCockroachExtension delegate = new TestcontainersCockroachExtension();

    @Override
    public @NotNull Class<TestcontainersCockroach> annotationType() {
        return TestcontainersCockroach.class;
    }

    @Override
    public @NotNull Class<? extends Annotation> containerAnnotationType() {
        return ContainerCockroach.class;
    }

    @Override
    public @NotNull Class<? extends Annotation> connectionAnnotationType() {
        return ConnectionCockroach.class;
    }

    @Override
    protected AbstractTestcontainersJdbcExtension<CockroachContainer, JdbcMetadata> delegate() {
        return delegate;
    }

    @Override
    protected JdbcMetadata metadata(@NotNull TestcontainersCockroach annotation) {
        return metadata(annotation.network(), annotation.image(), annotation.mode(), annotation.migration());
    }

    @Override
    protected Migration migration(@NotNull TestcontainersCockroach annotation) {
        return annotation.migration();
    }

    @Override
    protected Isolation isolationAnnotation(@NotNull TestcontainersCockroach annotation) {
        return annotation.isolation();
    }
}
