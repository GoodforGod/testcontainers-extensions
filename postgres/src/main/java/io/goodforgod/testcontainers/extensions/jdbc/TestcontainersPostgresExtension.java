package io.goodforgod.testcontainers.extensions.jdbc;

import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.utility.DockerImageName;

final class TestcontainersPostgresExtension extends AbstractTestcontainersJdbcExtension {

    private static final String PROTOCOL = "postgresql";

    private static final String EXTERNAL_JDBC_URL = "EXTERNAL_POSTGRES_URL";
    private static final String EXTERNAL_JDBC_USERNAME = "EXTERNAL_POSTGRES_USERNAME";
    private static final String EXTERNAL_JDBC_PASSWORD = "EXTERNAL_POSTGRES_PASSWORD";
    private static final String EXTERNAL_JDBC_HOST = "EXTERNAL_POSTGRES_HOST";
    private static final String EXTERNAL_JDBC_PORT = "EXTERNAL_POSTGRES_PORT";
    private static final String EXTERNAL_JDBC_DATABASE = "EXTERNAL_POSTGRES_DATABASE";

    @NotNull
    JdbcDatabaseContainer<?> getDefaultContainer(@NotNull String image) {
        var dockerImage = DockerImageName.parse(image)
                .asCompatibleSubstituteFor(DockerImageName.parse(PostgreSQLContainer.IMAGE));
        return new PostgreSQLContainer<>(dockerImage)
                .withDatabaseName("postgres")
                .withUsername("postgres")
                .withPassword("postgres")
                .withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger(PostgreSQLContainer.class))
                        .withMdc("image", image))
                .withNetwork(Network.SHARED);
    }

    @NotNull
    Optional<ContainerMetadata> findMetadata(@NotNull ExtensionContext context) {
        return findAnnotation(TestcontainersPostgres.class, context)
                .map(a -> new ContainerMetadata(a.image(), a.mode(), a.migration()));
    }

    @NotNull
    JdbcConnection getConnectionForContainer(@NotNull JdbcDatabaseContainer<?> container) {
        return JdbcConnectionImpl.forJDBC(container.getJdbcUrl(),
                container.getHost(),
                container.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT),
                container.getDatabaseName(),
                container.getUsername(),
                container.getPassword());
    }

    @NotNull
    Optional<JdbcConnection> getConnectionExternal() {
        var url = System.getenv(EXTERNAL_JDBC_URL);
        var host = System.getenv(EXTERNAL_JDBC_HOST);
        var port = System.getenv(EXTERNAL_JDBC_PORT);
        var user = System.getenv(EXTERNAL_JDBC_USERNAME);
        var password = System.getenv(EXTERNAL_JDBC_PASSWORD);

        var db = Optional.ofNullable(System.getenv(EXTERNAL_JDBC_DATABASE)).orElse("postgres");
        if (url != null) {
            if (host != null && port != null) {
                return Optional.of(JdbcConnectionImpl.forJDBC(url, host, Integer.parseInt(port), db, user, password));
            } else {
                return Optional.of(JdbcConnectionImpl.forJDBC(url, user, password));
            }
        } else if (host != null && port != null) {
            return Optional.of(JdbcConnectionImpl.forProtocol(PROTOCOL, host, Integer.parseInt(port), db, user, password));
        } else {
            return Optional.empty();
        }
    }
}
