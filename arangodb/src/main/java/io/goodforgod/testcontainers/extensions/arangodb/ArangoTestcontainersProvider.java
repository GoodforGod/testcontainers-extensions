package io.goodforgod.testcontainers.extensions.arangodb;

import io.goodforgod.testcontainers.extensions.ContainerContext;
import io.goodforgod.testcontainers.extensions.ContainerMode;
import io.goodforgod.testcontainers.extensions.TestcontainersProvider;
import io.testcontainers.arangodb.containers.ArangoContainer;
import java.lang.annotation.Annotation;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.testcontainers.containers.GenericContainer;

@Internal
public final class ArangoTestcontainersProvider implements TestcontainersProvider<TestcontainersArango, ArangoConnection> {

    private final TestcontainersArangoExtension delegate = new TestcontainersArangoExtension();

    @Override
    public @NotNull Class<TestcontainersArango> annotationType() {
        return TestcontainersArango.class;
    }

    @Override
    public @NotNull Class<? extends Annotation> containerAnnotationType() {
        return ContainerArango.class;
    }

    @Override
    public @NotNull Class<? extends Annotation> connectionAnnotationType() {
        return ConnectionArango.class;
    }

    @Override
    public @NotNull Class<ArangoConnection> connectionType() {
        return ArangoConnection.class;
    }

    @Override
    public @NotNull ContainerMode mode(@NotNull TestcontainersArango annotation) {
        return annotation.mode();
    }

    @Override
    public @NotNull String image(@NotNull TestcontainersArango annotation) {
        return annotation.image();
    }

    @Override
    public boolean networkShared(@NotNull TestcontainersArango annotation) {
        return annotation.network().shared();
    }

    @Override
    public String networkAlias(@NotNull TestcontainersArango annotation) {
        return annotation.network().alias();
    }

    @Override
    public @NotNull GenericContainer<?> createContainer(@NotNull TestcontainersArango annotation) {
        var metadata = new ArangoMetadata(annotation.network().shared(), annotation.network().alias(),
                annotation.image(), annotation.mode(), annotation.password());
        return delegate.createContainerDefault(metadata);
    }

    @Override
    public @NotNull ContainerContext<ArangoConnection> createContext(@NotNull GenericContainer<?> container) {
        return delegate.createContainerContext((ArangoContainer) container);
    }
}
