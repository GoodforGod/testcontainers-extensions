package io.goodforgod.testcontainers.extensions.microcks;

import io.github.microcks.testcontainers.MicrocksContainer;
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

@Internal
class TestcontainersMicrocksExtension extends
        AbstractTestcontainersExtension<MicrocksConnection, MicrocksContainer, MicrocksMetadata> {

    private static final String IMAGE_NAME = "quay.io/microcks/microcks-uber";

    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace
            .create(TestcontainersMicrocksExtension.class);

    protected Class<MicrocksContainer> getContainerType() {
        return MicrocksContainer.class;
    }

    protected Class<? extends Annotation> getContainerAnnotation() {
        return ContainerMicrocks.class;
    }

    protected Class<? extends Annotation> getConnectionAnnotation() {
        return ConnectionMicrocks.class;
    }

    @Override
    protected Class<MicrocksConnection> getConnectionType() {
        return MicrocksConnection.class;
    }

    @Override
    protected ExtensionContext.Namespace getNamespace() {
        return NAMESPACE;
    }

    @Override
    protected MicrocksContainer createContainerDefault(MicrocksMetadata metadata) {
        var image = DockerImageName.parse(metadata.image())
                .asCompatibleSubstituteFor(DockerImageName.parse(IMAGE_NAME));

        final MicrocksContainer container = new MicrocksContainer(image);
        final String alias = Optional.ofNullable(metadata.networkAlias())
                .orElseGet(() -> "microcks-" + System.currentTimeMillis());
        container.withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger(MicrocksContainer.class), true)
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
    protected ContainerContext<MicrocksConnection> createContainerContext(MicrocksContainer container) {
        return new MicrocksContext(container);
    }

    @NotNull
    protected Optional<MicrocksMetadata> findMetadata(@NotNull ExtensionContext context) {
        return findAnnotation(TestcontainersMicrocks.class, context)
                .map(a -> new MicrocksMetadata(a.network().shared(), a.network().alias(), a.image(), a.mode()));
    }
}
