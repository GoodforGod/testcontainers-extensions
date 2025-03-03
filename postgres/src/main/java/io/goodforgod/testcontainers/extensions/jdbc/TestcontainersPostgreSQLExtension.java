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
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.utility.DockerImageName;

final class TestcontainersPostgreSQLExtension extends
        AbstractTestcontainersJdbcExtension<PostgreSQLContainer<?>, JdbcMetadata> {

    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace
            .create(TestcontainersPostgreSQLExtension.class);

    @SuppressWarnings("unchecked")
    @Override
    protected Class<PostgreSQLContainer<?>> getContainerType() {
        return (Class<PostgreSQLContainer<?>>) ((Class<?>) PostgreSQLContainer.class);
    }

    @Override
    protected Class<? extends Annotation> getContainerAnnotation() {
        return ContainerPostgreSQL.class;
    }

    @Override
    protected Class<? extends Annotation> getConnectionAnnotation() {
        return ConnectionPostgreSQL.class;
    }

    @Override
    protected ExtensionContext.Namespace getNamespace() {
        return NAMESPACE;
    }

    @Override
    protected PostgreSQLContainer<?> createContainerDefault(JdbcMetadata metadata) {
        var image = DockerImageName.parse(metadata.image())
                .asCompatibleSubstituteFor(DockerImageName.parse(PostgreSQLContainer.IMAGE));

        final PostgreSQLContainer<?> container = new PostgreSQLContainer<>(image);
        final String alias = Optional.ofNullable(metadata.networkAlias())
                .orElseGet(() -> "postgres-" + System.currentTimeMillis());
        container.withDatabaseName("postgres");
        container.withUsername("postgres");
        container.withPassword("postgres");
        container.withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger(PostgreSQLContainer.class))
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
    protected ContainerContext<JdbcConnection> createContainerContext(PostgreSQLContainer<?> container) {
        return new PostgreSQLContext(container);
    }

    @NotNull
    protected Optional<JdbcMetadata> findMetadata(@NotNull ExtensionContext context) {
        return findAnnotation(TestcontainersPostgreSQL.class, context)
                .map(a -> new JdbcMetadata(a.network().shared(), a.network().alias(), a.image(), a.mode(), a.migration()));
    }
}
