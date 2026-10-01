package io.goodforgod.testcontainers.extensions.wiremock;

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
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.utility.DockerImageName;
import org.wiremock.integrations.testcontainers.WireMockContainer;

@Internal
class TestcontainersWireMockExtension extends
        AbstractTestcontainersExtension<WireMockConnection, WireMockContainer, WireMockMetadata> {

    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace
            .create(TestcontainersWireMockExtension.class);

    protected Class<WireMockContainer> getContainerType() {
        return WireMockContainer.class;
    }

    protected Class<? extends Annotation> getContainerAnnotation() {
        return ContainerWireMock.class;
    }

    protected Class<? extends Annotation> getConnectionAnnotation() {
        return ConnectionWireMock.class;
    }

    @Override
    protected Class<WireMockConnection> getConnectionType() {
        return WireMockConnection.class;
    }

    @Override
    protected ExtensionContext.Namespace getNamespace() {
        return NAMESPACE;
    }

    @Override
    protected WireMockContainer createContainerDefault(WireMockMetadata metadata) {
        var image = DockerImageName.parse(metadata.image())
                .asCompatibleSubstituteFor(DockerImageName.parse(WireMockContainer.OFFICIAL_IMAGE_NAME));

        final WireMockContainer container = new WireMockContainer(image);
        final String alias = Optional.ofNullable(metadata.networkAlias())
                .orElseGet(() -> "wiremock-" + System.currentTimeMillis());
        container.withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger(WireMockContainer.class), true)
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
    protected ContainerContext<WireMockConnection> createContainerContext(WireMockContainer container) {
        return new WireMockContext(container);
    }

    @NotNull
    protected Optional<WireMockMetadata> findMetadata(@NotNull ExtensionContext context) {
        return findAnnotation(TestcontainersWireMock.class, context)
                .map(a -> new WireMockMetadata(a.network().shared(), a.network().alias(), a.image(), a.mode()));
    }
}
