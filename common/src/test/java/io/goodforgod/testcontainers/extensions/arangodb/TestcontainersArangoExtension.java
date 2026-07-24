package io.goodforgod.testcontainers.extensions.arangodb;

import io.goodforgod.testcontainers.extensions.AbstractTestcontainersExtension;
import io.goodforgod.testcontainers.extensions.ContainerContext;
import io.testcontainers.arangodb.containers.ArangoContainer;
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
class TestcontainersArangoExtension extends
        AbstractTestcontainersExtension<ArangoConnection, ArangoContainer, ArangoMetadata> {

    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace
            .create(TestcontainersArangoExtension.class);

    @Override
    protected Class<ArangoContainer> getContainerType() {
        return ArangoContainer.class;
    }

    @Override
    protected Class<? extends Annotation> getContainerAnnotation() {
        return ContainerArango.class;
    }

    @Override
    protected Class<? extends Annotation> getConnectionAnnotation() {
        return ConnectionArango.class;
    }

    @Override
    protected Class<ArangoConnection> getConnectionType() {
        return ArangoConnection.class;
    }

    @Override
    protected ExtensionContext.Namespace getNamespace() {
        return NAMESPACE;
    }

    @Override
    protected ArangoContainer createContainerDefault(ArangoMetadata metadata) {
        var image = DockerImageName.parse(metadata.image())
                .asCompatibleSubstituteFor(DockerImageName.parse("arangodb"));

        final ArangoContainer container = new ArangoContainer(image);
        if (metadata.password() == null || metadata.password().isBlank()) {
            container.withoutAuth();
        } else {
            container.withPassword(metadata.password());
        }

        final String alias = Optional.ofNullable(metadata.networkAlias())
                .orElseGet(() -> "arangodb-" + System.currentTimeMillis());
        container.withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger(ArangoContainer.class), true)
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
    protected ContainerContext<ArangoConnection> createContainerContext(ArangoContainer container) {
        return new ArangoContext(container);
    }

    @NotNull
    protected Optional<ArangoMetadata> findMetadata(@NotNull ExtensionContext context) {
        return findAnnotation(TestcontainersArango.class, context)
                .map(a -> new ArangoMetadata(a.network().shared(), a.network().alias(), a.image(), a.mode(),
                        a.password()));
    }
}
