package io.goodforgod.testcontainers.extensions.mockserver;

import io.goodforgod.testcontainers.extensions.AbstractTestcontainersExtension;
import io.goodforgod.testcontainers.extensions.ContainerContext;
import java.lang.annotation.Annotation;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.MockServerContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.utility.DockerImageName;

@Internal
class TestcontainersMockServerExtension extends
        AbstractTestcontainersExtension<MockServerConnection, MockServerContainer, MockServerMetadata> {

    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace
            .create(TestcontainersMockServerExtension.class);

    protected Class<MockServerContainer> getContainerType() {
        return MockServerContainer.class;
    }

    protected Class<? extends Annotation> getContainerAnnotation() {
        return ContainerMockServer.class;
    }

    protected Class<? extends Annotation> getConnectionAnnotation() {
        return ConnectionMockServer.class;
    }

    @Override
    protected Class<MockServerConnection> getConnectionType() {
        return MockServerConnection.class;
    }

    @Override
    protected ExtensionContext.Namespace getNamespace() {
        return NAMESPACE;
    }

    @Override
    protected MockServerContainer createContainerDefault(MockServerMetadata metadata) {
        var image = DockerImageName.parse(metadata.image())
                .asCompatibleSubstituteFor(DockerImageName.parse("mockserver/mockserver"));

        final MockServerContainer container = new MockServerContainer(image);
        final String alias = Optional.ofNullable(metadata.networkAlias())
                .orElseGet(() -> "mockserver-" + System.currentTimeMillis());
        container.withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger(MockServerContainer.class), true)
                .withMdc("image", image.asCanonicalNameString())
                .withMdc("alias", alias));
        container.withStartupTimeout(Duration.ofMinutes(2));
        container.setNetworkAliases(new ArrayList<>(List.of(alias)));
        if (metadata.networkShared()) {
            container.withNetwork(Network.SHARED);
        }

        return container;
    }

    @Override
    protected ContainerContext<MockServerConnection> createContainerContext(MockServerContainer container) {
        return new MockServerContext(container);
    }

    @NotNull
    protected Optional<MockServerMetadata> findMetadata(@NotNull ExtensionContext context) {
        return findAnnotation(TestcontainersMockServer.class, context)
                .map(a -> new MockServerMetadata(a.network().shared(), a.network().alias(), a.image(), a.mode()));
    }
}
