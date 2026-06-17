package io.goodforgod.testcontainers.extensions.valkey;

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
class TestcontainersValkeyExtension extends
        AbstractTestcontainersExtension<ValkeyConnection, ValkeyContainer, ValkeyMetadata> {

    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace
            .create(TestcontainersValkeyExtension.class);

    protected Class<ValkeyContainer> getContainerType() {
        return ValkeyContainer.class;
    }

    protected Class<? extends Annotation> getContainerAnnotation() {
        return ContainerValkey.class;
    }

    protected Class<? extends Annotation> getConnectionAnnotation() {
        return ConnectionValkey.class;
    }

    @Override
    protected Class<ValkeyConnection> getConnectionType() {
        return ValkeyConnection.class;
    }

    @Override
    protected ExtensionContext.Namespace getNamespace() {
        return NAMESPACE;
    }

    @Override
    protected ValkeyContainer createContainerDefault(ValkeyMetadata metadata) {
        var image = DockerImageName.parse(metadata.image())
                .asCompatibleSubstituteFor(DockerImageName.parse("valkey/valkey"));

        final ValkeyContainer container = new ValkeyContainer(image)
                .waitAfterStart(Duration.ofMillis(25)); // some drivers can fail to connect on hot start
        final String alias = Optional.ofNullable(metadata.networkAlias()).orElseGet(() -> "valkey-" + System.currentTimeMillis());
        container.withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger(ValkeyContainer.class), true)
                .withMdc("image", image.asCanonicalNameString())
                .withMdc("alias", alias))
                .withStartupTimeout(Duration.ofMinutes(2));
        container.setNetworkAliases(new ArrayList<>(List.of(alias)));
        if (metadata.networkShared()) {
            container.withNetwork(Network.SHARED);
        }

        return container;
    }

    @Override
    protected ContainerContext<ValkeyConnection> createContainerContext(ValkeyContainer container) {
        return new ValkeyContext(container);
    }

    @NotNull
    protected Optional<ValkeyMetadata> findMetadata(@NotNull ExtensionContext context) {
        return findAnnotation(TestcontainersValkey.class, context)
                .map(a -> new ValkeyMetadata(a.network().shared(), a.network().alias(), a.image(), a.mode()));
    }
}
