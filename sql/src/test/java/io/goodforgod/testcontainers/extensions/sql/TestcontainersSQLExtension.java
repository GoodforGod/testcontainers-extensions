package io.goodforgod.testcontainers.extensions.sql;

import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.utility.DockerImageName;

final class TestcontainersSQLExtension extends AbstractTestcontainersSQLExtension {

    @NotNull
    JdbcDatabaseContainer<?> getDefaultContainer(@NotNull String image) {
        var dockerImage = DockerImageName.parse(image)
                .asCompatibleSubstituteFor(DockerImageName.parse(PostgreSQLContainer.IMAGE));
        return new PostgreSQLContainer<>(dockerImage)
                .withDatabaseName("postgres")
                .withUsername("postgres")
                .withPassword("postgres")
                .withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger(PostgreSQLContainer.class)).withMdc("image", image))
                .withNetwork(Network.SHARED);
    }

    @NotNull
    Optional<ContainerMetadata> findMetadata(@NotNull ExtensionContext context) {
        return findAnnotation(TestcontainersSQL.class, context)
                .map(a -> new ContainerMetadata(a.image(), a.mode(), a.migration()));
    }

    @NotNull
    SqlConnection getConnection(@NotNull JdbcDatabaseContainer<?> container) {
        return new SqlConnectionImpl(container, PostgreSQLContainer.POSTGRESQL_PORT);
    }
}
