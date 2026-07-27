package io.goodforgod.testcontainers.extensions.mockserver;

import io.goodforgod.testcontainers.extensions.ContainerContext;
import io.goodforgod.testcontainers.extensions.ContainerMode;
import io.goodforgod.testcontainers.extensions.TestcontainersProvider;
import java.lang.annotation.Annotation;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MockServerContainer;

@Internal
public final class MockServerTestcontainersProvider implements
        TestcontainersProvider<TestcontainersMockServer, MockServerConnection> {

    private final TestcontainersMockServerExtension delegate = new TestcontainersMockServerExtension();

    @Override
    public @NotNull Class<TestcontainersMockServer> annotationType() {
        return TestcontainersMockServer.class;
    }

    @Override
    public @NotNull Class<? extends Annotation> containerAnnotationType() {
        return ContainerMockServer.class;
    }

    @Override
    public @NotNull Class<? extends Annotation> connectionAnnotationType() {
        return ConnectionMockServer.class;
    }

    @Override
    public @NotNull Class<MockServerConnection> connectionType() {
        return MockServerConnection.class;
    }

    @Override
    public @NotNull ContainerMode mode(@NotNull TestcontainersMockServer annotation) {
        return annotation.mode();
    }

    @Override
    public @NotNull String image(@NotNull TestcontainersMockServer annotation) {
        return annotation.image();
    }

    @Override
    public boolean networkShared(@NotNull TestcontainersMockServer annotation) {
        return annotation.network().shared();
    }

    @Override
    public String networkAlias(@NotNull TestcontainersMockServer annotation) {
        return annotation.network().alias();
    }

    @Override
    public @NotNull GenericContainer<?> createContainer(@NotNull TestcontainersMockServer annotation) {
        var metadata = new MockServerMetadata(annotation.network().shared(), annotation.network().alias(), annotation.image(),
                annotation.mode());
        return delegate.createContainerDefault(metadata);
    }

    @Override
    public @NotNull ContainerContext<MockServerConnection> createContext(@NotNull GenericContainer<?> container) {
        return delegate.createContainerContext((MockServerContainer) container);
    }
}
