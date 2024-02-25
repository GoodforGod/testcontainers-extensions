package io.goodforgod.testcontainers.extensions.jdbc;

import io.goodforgod.testcontainers.extensions.ContainerContext;
import io.goodforgod.testcontainers.extensions.jdbc.example.ContainerJdbc;
import io.goodforgod.testcontainers.extensions.jdbc.example.ContainerJdbcConnection;
import io.goodforgod.testcontainers.extensions.jdbc.example.TestcontainersJdbc;
import java.lang.annotation.Annotation;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.utility.DockerImageName;

public final class PostgresJdbcTestcontainersJdbcExtension extends
        AbstractTestcontainersJdbcExtension<PostgreSQLContainer<?>, PostgresJdbcMetadata> {

    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace
            .create(PostgresJdbcTestcontainersJdbcExtension.class);

    @Override
    protected ExtensionContext.Namespace getNamespace() {
        return NAMESPACE;
    }

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

    @Override
    protected PostgreSQLContainer<?> createContainerDefault(PostgresJdbcMetadata metadata) {
        var dockerImage = DockerImageName.parse(metadata.image())
                .asCompatibleSubstituteFor(DockerImageName.parse(PostgreSQLContainer.IMAGE));

        String alias = "postgres-" + System.currentTimeMillis();
        return new PostgreSQLContainer<>(dockerImage)
                .withDatabaseName("postgres")
                .withUsername("postgres")
                .withPassword("postgres")
                .withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger(PostgreSQLContainer.class))
                        .withMdc("image", metadata.image()))
                .withNetworkAliases(alias);
    }

    @Override
    protected ContainerContext<JdbcConnection> createContainerContext(PostgreSQLContainer<?> container) {
        return new PostgresJdbcContext(container);
    }

    @NotNull
    protected Optional<PostgresJdbcMetadata> findMetadata(@NotNull ExtensionContext context) {
        return findAnnotation(TestcontainersJdbc.class, context)
                .map(a -> new PostgresJdbcMetadata(false, null, a.image(), a.mode(), a.migration()));
    }
}
