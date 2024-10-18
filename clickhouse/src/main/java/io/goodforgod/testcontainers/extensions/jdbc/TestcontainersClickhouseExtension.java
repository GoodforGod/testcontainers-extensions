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
import org.testcontainers.clickhouse.ClickHouseContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.utility.DockerImageName;

final class TestcontainersClickhouseExtension extends
        AbstractTestcontainersJdbcExtension<ClickHouseContainer, JdbcMetadata> {

    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace
            .create(TestcontainersClickhouseExtension.class);

    @Override
    protected Class<ClickHouseContainer> getContainerType() {
        return ClickHouseContainer.class;
    }

    @Override
    protected Class<? extends Annotation> getContainerAnnotation() {
        return ContainerClickhouse.class;
    }

    @Override
    protected Class<? extends Annotation> getConnectionAnnotation() {
        return ConnectionClickhouse.class;
    }

    @Override
    protected ExtensionContext.Namespace getNamespace() {
        return NAMESPACE;
    }

    @Override
    protected ClickHouseContainer createContainerDefault(JdbcMetadata metadata) {
        var image = DockerImageName.parse(metadata.image())
                .asCompatibleSubstituteFor(DockerImageName.parse("yandex/clickhouse-server"));

        final ClickHouseContainer container = new ClickHouseContainer(image);
        final String alias = Optional.ofNullable(metadata.networkAlias())
                .orElseGet(() -> "clickhouse-" + System.currentTimeMillis());
        container.withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger(ClickHouseContainer.class))
                .withMdc("image", image.asCanonicalNameString())
                .withMdc("alias", alias));
        container.withDatabaseName("clickhouse");
        container.withUsername("clickhouse");
        container.withPassword("clickhouse");
        container.withStartupTimeout(Duration.ofSeconds(15));
        container.setNetworkAliases(new ArrayList<>(List.of(alias)));
        if (metadata.networkShared()) {
            container.withNetwork(Network.SHARED);
        }

        return container;
    }

    @Override
    protected ContainerContext<JdbcConnection> createContainerContext(ClickHouseContainer container) {
        return new ClickhouseContext(container);
    }

    @NotNull
    protected Optional<JdbcMetadata> findMetadata(@NotNull ExtensionContext context) {
        return findAnnotation(TestcontainersClickhouse.class, context)
                .map(a -> new JdbcMetadata(a.network().shared(), a.network().alias(), a.image(), a.mode(), a.migration()));
    }
}
