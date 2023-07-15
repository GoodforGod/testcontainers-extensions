package io.goodforgod.testcontainers.extensions.jdbc;

import java.lang.annotation.Annotation;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.utility.DockerImageName;

final class TestcontainersJdbcExtension extends AbstractTestcontainersJdbcExtension<PostgreSQLContainer<?>> {

    @SuppressWarnings("unchecked")
    @Override
    protected Class<PostgreSQLContainer<?>> getContainerType() {
        return (Class<PostgreSQLContainer<?>>) ((Class<?>) PostgreSQLContainer.class);
    }

    @Override
    protected Class<? extends Annotation> getContainerAnnotation() {
        return ContainerJdbc.class;
    }

    @Override
    protected Class<? extends Annotation> getConnectionAnnotation() {
        return ContainerJdbcConnection.class;
    }

    @NotNull
    protected PostgreSQLContainer<?> getDefaultContainer(@NotNull String image) {
        var dockerImage = DockerImageName.parse(image)
                .asCompatibleSubstituteFor(DockerImageName.parse(PostgreSQLContainer.IMAGE));

        var alias = "postgres-" + System.currentTimeMillis();
        return new PostgreSQLContainer<>(dockerImage)
                .withDatabaseName("postgres")
                .withUsername("postgres")
                .withPassword("postgres")
                .withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger(PostgreSQLContainer.class))
                        .withMdc("image", image)
                        .withMdc("alias", alias))
                .withNetworkAliases(alias)
                .withNetwork(Network.SHARED);
    }

    @NotNull
    protected Optional<ContainerMetadata> findMetadata(@NotNull ExtensionContext context) {
        return findAnnotation(TestcontainersJdbc.class, context)
                .map(a -> new ContainerMetadata(a.image(), a.mode(), a.migration()));
    }

    @NotNull
    protected JdbcConnection getConnectionForContainer(@NotNull PostgreSQLContainer<?> container) {
        return JdbcConnectionImpl.forJDBC(container.getJdbcUrl(),
                container.getHost(),
                container.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT),
                container.getNetworkAliases().get(container.getNetworkAliases().size() - 1),
                PostgreSQLContainer.POSTGRESQL_PORT,
                container.getDatabaseName(),
                container.getUsername(),
                container.getPassword());
    }

    @Override
    @NotNull
    protected Optional<JdbcConnection> getConnectionExternal() {
        return Optional.empty();
    }
}
