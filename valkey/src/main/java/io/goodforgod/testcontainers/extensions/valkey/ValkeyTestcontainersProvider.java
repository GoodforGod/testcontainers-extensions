package io.goodforgod.testcontainers.extensions.valkey;

import io.goodforgod.testcontainers.extensions.ContainerContext;
import io.goodforgod.testcontainers.extensions.ContainerMode;
import io.goodforgod.testcontainers.extensions.TestcontainersProvider;
import java.lang.annotation.Annotation;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.testcontainers.containers.GenericContainer;

@Internal
public final class ValkeyTestcontainersProvider implements TestcontainersProvider<TestcontainersValkey, ValkeyConnection> {

    private final TestcontainersValkeyExtension delegate = new TestcontainersValkeyExtension();

    @Override
    public @NotNull Class<TestcontainersValkey> annotationType() {
        return TestcontainersValkey.class;
    }

    @Override
    public @NotNull Class<? extends Annotation> containerAnnotationType() {
        return ContainerValkey.class;
    }

    @Override
    public @NotNull Class<? extends Annotation> connectionAnnotationType() {
        return ConnectionValkey.class;
    }

    @Override
    public @NotNull Class<ValkeyConnection> connectionType() {
        return ValkeyConnection.class;
    }

    @Override
    public @NotNull ContainerMode mode(@NotNull TestcontainersValkey annotation) {
        return annotation.mode();
    }

    @Override
    public @NotNull String image(@NotNull TestcontainersValkey annotation) {
        return annotation.image();
    }

    @Override
    public boolean networkShared(@NotNull TestcontainersValkey annotation) {
        return annotation.network().shared();
    }

    @Override
    public String networkAlias(@NotNull TestcontainersValkey annotation) {
        return annotation.network().alias();
    }

    @Override
    public @NotNull GenericContainer<?> createContainer(@NotNull TestcontainersValkey annotation) {
        var metadata = new ValkeyMetadata(annotation.network().shared(), annotation.network().alias(), annotation.image(),
                annotation.mode());
        return delegate.createContainerDefault(metadata);
    }

    @Override
    public @NotNull ContainerContext<ValkeyConnection> createContext(@NotNull GenericContainer<?> container) {
        return delegate.createContainerContext((ValkeyContainer) container);
    }
}
