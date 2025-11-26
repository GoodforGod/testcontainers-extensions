package io.goodforgod.testcontainers.extensions.jdbc;

import io.goodforgod.testcontainers.extensions.ContainerContext;
import java.lang.annotation.Annotation;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.CockroachContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.utility.DockerImageName;

final class TestcontainersCockroachExtension extends
        AbstractTestcontainersJdbcExtension<CockroachContainer, JdbcMetadata> {

    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace
            .create(TestcontainersCockroachExtension.class);

    @Override
    protected Class<CockroachContainer> getContainerType() {
        return CockroachContainer.class;
    }

    @Override
    protected Class<? extends Annotation> getContainerAnnotation() {
        return ContainerCockroach.class;
    }

    @Override
    protected Class<? extends Annotation> getConnectionAnnotation() {
        return ConnectionCockroach.class;
    }

    @Override
    protected ExtensionContext.Namespace getNamespace() {
        return NAMESPACE;
    }

    @Override
    protected CockroachContainer createContainerDefault(JdbcMetadata metadata) {
        var image = DockerImageName.parse(metadata.image())
                .asCompatibleSubstituteFor(DockerImageName.parse("cockroachdb/cockroach"));

        final CockroachContainer container = new CockroachContainer(image);
        final String alias = Optional.ofNullable(metadata.networkAlias())
                .orElseGet(() -> "cockroach-" + System.currentTimeMillis());
        container.withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger(CockroachContainer.class), true)
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
    protected ContainerContext<JdbcConnection> createContainerContext(CockroachContainer container) {
        return new CockroachContext(container);
    }

    @NotNull
    protected Optional<JdbcMetadata> findMetadata(@NotNull ExtensionContext context) {
        return findAnnotation(TestcontainersCockroach.class, context)
                .map(a -> new JdbcMetadata(a.network().shared(), a.network().alias(), a.image(), a.mode(), a.migration()));
    }
}
