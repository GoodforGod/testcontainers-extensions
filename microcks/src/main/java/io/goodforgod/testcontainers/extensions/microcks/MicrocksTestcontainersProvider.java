package io.goodforgod.testcontainers.extensions.microcks;

import io.github.microcks.testcontainers.MicrocksContainer;
import io.goodforgod.testcontainers.extensions.ContainerContext;
import io.goodforgod.testcontainers.extensions.ContainerMode;
import io.goodforgod.testcontainers.extensions.TestcontainersProvider;
import java.lang.annotation.Annotation;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.testcontainers.containers.GenericContainer;

@Internal
public final class MicrocksTestcontainersProvider implements
        TestcontainersProvider<TestcontainersMicrocks, MicrocksConnection> {

    private final TestcontainersMicrocksExtension delegate = new TestcontainersMicrocksExtension();

    @Override
    public @NotNull Class<TestcontainersMicrocks> annotationType() {
        return TestcontainersMicrocks.class;
    }

    @Override
    public @NotNull Class<? extends Annotation> containerAnnotationType() {
        return ContainerMicrocks.class;
    }

    @Override
    public @NotNull Class<? extends Annotation> connectionAnnotationType() {
        return ConnectionMicrocks.class;
    }

    @Override
    public @NotNull Class<MicrocksConnection> connectionType() {
        return MicrocksConnection.class;
    }

    @Override
    public @NotNull ContainerMode mode(@NotNull TestcontainersMicrocks annotation) {
        return annotation.mode();
    }

    @Override
    public @NotNull String image(@NotNull TestcontainersMicrocks annotation) {
        return annotation.image();
    }

    @Override
    public boolean networkShared(@NotNull TestcontainersMicrocks annotation) {
        return annotation.network().shared();
    }

    @Override
    public String networkAlias(@NotNull TestcontainersMicrocks annotation) {
        return annotation.network().alias();
    }

    @Override
    public @NotNull GenericContainer<?> createContainer(@NotNull TestcontainersMicrocks annotation) {
        var metadata = new MicrocksMetadata(annotation.network().shared(), annotation.network().alias(), annotation.image(),
                annotation.mode());
        return delegate.createContainerDefault(metadata);
    }

    @Override
    public @NotNull ContainerContext<MicrocksConnection> createContext(@NotNull GenericContainer<?> container) {
        return delegate.createContainerContext((MicrocksContainer) container);
    }
}
