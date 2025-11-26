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
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

final class TestcontainersMySQLExtension extends AbstractTestcontainersJdbcExtension<MySQLContainer<?>, JdbcMetadata> {

    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace
            .create(TestcontainersMySQLExtension.class);

    @SuppressWarnings("unchecked")
    @Override
    protected Class<MySQLContainer<?>> getContainerType() {
        return (Class<MySQLContainer<?>>) ((Class<?>) MySQLContainer.class);
    }

    @Override
    protected Class<? extends Annotation> getContainerAnnotation() {
        return ContainerMySQL.class;
    }

    @Override
    protected Class<? extends Annotation> getConnectionAnnotation() {
        return ConnectionMySQL.class;
    }

    @Override
    protected ExtensionContext.Namespace getNamespace() {
        return NAMESPACE;
    }

    @Override
    protected MySQLContainer<?> createContainerDefault(JdbcMetadata metadata) {
        var image = DockerImageName.parse(metadata.image())
                .asCompatibleSubstituteFor(DockerImageName.parse(org.testcontainers.containers.MySQLContainer.NAME));

        final MySQLContainer<?> container = new MySQLContainer<>(image);
        final String alias = Optional.ofNullable(metadata.networkAlias()).orElseGet(() -> "mysql-" + System.currentTimeMillis());
        container.withDatabaseName(MySQLContext.DATABASE_NAME);
        container.withUsername("mysql");
        container.withPassword("mysql");
        container.withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger(MySQLContainer.class), true)
                .withMdc("image", image.asCanonicalNameString())
                .withMdc("alias", alias));
        container.waitingFor(Wait.forListeningPort());
        container.withStartupTimeout(Duration.ofMinutes(2));
        container.setNetworkAliases(new ArrayList<>(List.of(alias)));
        if (metadata.networkShared()) {
            container.withNetwork(Network.SHARED);
        }

        return container;
    }

    @Override
    protected ContainerContext<JdbcConnection> createContainerContext(MySQLContainer<?> container) {
        return new MySQLContext(container);
    }

    @NotNull
    protected Optional<JdbcMetadata> findMetadata(@NotNull ExtensionContext context) {
        return findAnnotation(TestcontainersMySQL.class, context)
                .map(a -> new JdbcMetadata(a.network().shared(), a.network().alias(), a.image(), a.mode(), a.migration()));
    }
}
