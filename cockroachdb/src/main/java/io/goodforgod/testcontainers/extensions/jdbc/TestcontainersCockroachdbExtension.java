package io.goodforgod.testcontainers.extensions.jdbc;

import java.lang.annotation.Annotation;
import java.time.Duration;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.CockroachContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.utility.DockerImageName;

final class TestcontainersCockroachdbExtension extends AbstractTestcontainersJdbcExtension<CockroachContainer> {

    private static final String PROTOCOL = "postgresql";
    private static final int PORT = 26257;

    private static final String EXTERNAL_TEST_COCKROACHDB_JDBC_URL = "EXTERNAL_TEST_COCKROACHDB_JDBC_URL";
    private static final String EXTERNAL_TEST_COCKROACHDB_USERNAME = "EXTERNAL_TEST_COCKROACHDB_USERNAME";
    private static final String EXTERNAL_TEST_COCKROACHDB_PASSWORD = "EXTERNAL_TEST_COCKROACHDB_PASSWORD";
    private static final String EXTERNAL_TEST_COCKROACHDB_HOST = "EXTERNAL_TEST_COCKROACHDB_HOST";
    private static final String EXTERNAL_TEST_COCKROACHDB_PORT = "EXTERNAL_TEST_COCKROACHDB_PORT";
    private static final String EXTERNAL_TEST_COCKROACHDB_DATABASE = "EXTERNAL_TEST_COCKROACHDB_DATABASE";

    @Override
    protected Class<CockroachContainer> getContainerType() {
        return CockroachContainer.class;
    }

    @Override
    protected Class<? extends Annotation> getContainerAnnotation() {
        return ContainerCockroachdb.class;
    }

    @Override
    protected Class<? extends Annotation> getConnectionAnnotation() {
        return ContainerCockroachdbConnection.class;
    }

    @NotNull
    @Override
    protected CockroachContainer getDefaultContainer(@NotNull ContainerMetadata metadata) {
        var dockerImage = DockerImageName.parse(metadata.image())
                .asCompatibleSubstituteFor(DockerImageName.parse("cockroachdb/cockroach"));

        var alias = "cockroachdb-" + System.currentTimeMillis();
        return new CockroachContainer(dockerImage)
                .withDatabaseName("cockroachdb")
                .withUsername("cockroachdb")
                .withPassword("cockroachdb")
                .withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger(CockroachContainer.class))
                        .withMdc("image", metadata.image())
                        .withMdc("alias", alias))
                .withNetworkAliases(alias)
                .withNetwork(Network.SHARED)
                .withStartupTimeout(Duration.ofMinutes(5));
    }

    @NotNull
    protected Optional<ContainerMetadata> findMetadata(@NotNull ExtensionContext context) {
        return findAnnotation(TestcontainersCockroachdb.class, context)
                .map(a -> new ContainerMetadata(a.image(), a.mode(), a.migration()));
    }

    @NotNull
    protected JdbcConnection getConnectionForContainer(@NotNull CockroachContainer container) {
        final String alias = container.getNetworkAliases().stream()
                .filter(a -> a.startsWith("cockroachdb"))
                .findFirst()
                .or(() -> (container.getNetworkAliases().isEmpty())
                        ? Optional.empty()
                        : Optional.of(container.getNetworkAliases().get(container.getNetworkAliases().size() - 1)))
                .orElse(null);

        return JdbcConnectionImpl.forJDBC(container.getJdbcUrl(),
                container.getHost(),
                container.getMappedPort(PORT),
                alias,
                PORT,
                container.getDatabaseName(),
                container.getUsername(),
                container.getPassword());
    }

    @NotNull
    protected Optional<JdbcConnection> getConnectionExternal() {
        var url = System.getenv(EXTERNAL_TEST_COCKROACHDB_JDBC_URL);
        var host = System.getenv(EXTERNAL_TEST_COCKROACHDB_HOST);
        var port = System.getenv(EXTERNAL_TEST_COCKROACHDB_PORT);
        var user = System.getenv(EXTERNAL_TEST_COCKROACHDB_USERNAME);
        var password = System.getenv(EXTERNAL_TEST_COCKROACHDB_PASSWORD);

        var db = Optional.ofNullable(System.getenv(EXTERNAL_TEST_COCKROACHDB_DATABASE)).orElse("cockroachdb");
        if (url != null) {
            if (host != null && port != null) {
                return Optional.of(JdbcConnectionImpl.forJDBC(url, host, Integer.parseInt(port), null, null, db, user, password));
            } else {
                return Optional.of(JdbcConnectionImpl.forExternal(url, user, password));
            }
        } else if (host != null && port != null) {
            return Optional.of(JdbcConnectionImpl.forProtocol(PROTOCOL, host, Integer.parseInt(port), db, user, password));
        } else {
            return Optional.empty();
        }
    }
}
