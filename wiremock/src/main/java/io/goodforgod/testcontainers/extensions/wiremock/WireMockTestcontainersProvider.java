package io.goodforgod.testcontainers.extensions.wiremock;

import io.goodforgod.testcontainers.extensions.ContainerContext;
import io.goodforgod.testcontainers.extensions.ContainerMode;
import io.goodforgod.testcontainers.extensions.TestcontainersProvider;
import java.lang.annotation.Annotation;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.testcontainers.containers.GenericContainer;
import org.wiremock.integrations.testcontainers.WireMockContainer;

@Internal
public final class WireMockTestcontainersProvider implements
        TestcontainersProvider<TestcontainersWireMock, WireMockConnection> {

    private final TestcontainersWireMockExtension delegate = new TestcontainersWireMockExtension();

    @Override
    public @NotNull Class<TestcontainersWireMock> annotationType() {
        return TestcontainersWireMock.class;
    }

    @Override
    public @NotNull Class<? extends Annotation> containerAnnotationType() {
        return ContainerWireMock.class;
    }

    @Override
    public @NotNull Class<? extends Annotation> connectionAnnotationType() {
        return ConnectionWireMock.class;
    }

    @Override
    public @NotNull Class<WireMockConnection> connectionType() {
        return WireMockConnection.class;
    }

    @Override
    public @NotNull ContainerMode mode(@NotNull TestcontainersWireMock annotation) {
        return annotation.mode();
    }

    @Override
    public @NotNull String image(@NotNull TestcontainersWireMock annotation) {
        return annotation.image();
    }

    @Override
    public boolean networkShared(@NotNull TestcontainersWireMock annotation) {
        return annotation.network().shared();
    }

    @Override
    public String networkAlias(@NotNull TestcontainersWireMock annotation) {
        return annotation.network().alias();
    }

    @Override
    public @NotNull GenericContainer<?> createContainer(@NotNull TestcontainersWireMock annotation) {
        var metadata = new WireMockMetadata(annotation.network().shared(), annotation.network().alias(), annotation.image(),
                annotation.mode());
        return delegate.createContainerDefault(metadata);
    }

    @Override
    public @NotNull ContainerContext<WireMockConnection> createContext(@NotNull GenericContainer<?> container) {
        return delegate.createContainerContext((WireMockContainer) container);
    }
}
