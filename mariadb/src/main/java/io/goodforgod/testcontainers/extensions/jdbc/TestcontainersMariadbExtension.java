package io.goodforgod.testcontainers.extensions.jdbc;

import java.lang.annotation.Annotation;
import java.time.Duration;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

final class TestcontainersMariadbExtension extends AbstractTestcontainersJdbcExtension<MariaDBContainer<?>, MariadbMetadata> {

    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace
            .create(TestcontainersMariadbExtension.class);

    private static final String PROTOCOL = "mariadb";
    private static final String DATABASE_NAME = "mariadb";
    private static final Integer MARIADB_PORT = 3306;

    private static final String EXTERNAL_TEST_MARIADB_JDBC_URL = "EXTERNAL_TEST_MARIADB_JDBC_URL";
    private static final String EXTERNAL_TEST_MARIADB_USERNAME = "EXTERNAL_TEST_MARIADB_USERNAME";
    private static final String EXTERNAL_TEST_MARIADB_PASSWORD = "EXTERNAL_TEST_MARIADB_PASSWORD";
    private static final String EXTERNAL_TEST_MARIADB_HOST = "EXTERNAL_TEST_MARIADB_HOST";
    private static final String EXTERNAL_TEST_MARIADB_PORT = "EXTERNAL_TEST_MARIADB_PORT";
    private static final String EXTERNAL_TEST_MARIADB_DATABASE = "EXTERNAL_TEST_MARIADB_DATABASE";

    @SuppressWarnings("unchecked")
    @Override
    protected Class<MariaDBContainer<?>> getContainerType() {
        return (Class<MariaDBContainer<?>>) ((Class<?>) MariaDBContainer.class);
    }

    @Override
    protected Class<? extends Annotation> getContainerAnnotation() {
        return ContainerMariadb.class;
    }

    @Override
    protected Class<? extends Annotation> getConnectionAnnotation() {
        return ContainerMariadbConnection.class;
    }

    @Override
    protected MariaDBContainer<?> getContainerDefault(MariadbMetadata metadata) {
        var dockerImage = DockerImageName.parse(metadata.image())
                .asCompatibleSubstituteFor(DockerImageName.parse(MariaDBContainer.NAME));

        var container = new MariaDBContainer<>(dockerImage)
                .withDatabaseName(DATABASE_NAME)
                .withUsername("mariadb")
                .withPassword("mariadb")
                .withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger(MariaDBContainer.class))
                        .withMdc("image", metadata.image())
                        .withMdc("alias", metadata.networkAliasOrDefault()))
                .withNetworkAliases(metadata.networkAliasOrDefault())
                .waitingFor(Wait.forListeningPort())
                .withStartupTimeout(Duration.ofMinutes(5));

        if (metadata.networkShared()) {
            container.withNetwork(Network.SHARED);
        }

        return container;
    }

    @Override
    protected ExtensionContext.Namespace getNamespace() {
        return NAMESPACE;
    }

    @NotNull
    protected Optional<MariadbMetadata> findMetadata(@NotNull ExtensionContext context) {
        return findAnnotation(TestcontainersMariadb.class, context)
                .map(a -> new MariadbMetadata(a.network().shared(), a.network().alias(), a.image(), a.mode(), a.migration()));
    }

    @NotNull
    protected JdbcConnection getConnectionForContainer(MariadbMetadata metadata, @NotNull MariaDBContainer<?> container) {
        final String alias = container.getNetworkAliases().stream()
                .filter(a -> a.equals(metadata.networkAliasOrDefault()))
                .findFirst()
                .or(() -> container.getNetworkAliases().stream().findFirst())
                .orElse(null);

        return JdbcConnectionImpl.forJDBC(container.getJdbcUrl(),
                container.getHost(),
                container.getMappedPort(MARIADB_PORT),
                alias,
                MARIADB_PORT,
                container.getDatabaseName(),
                container.getUsername(),
                container.getPassword());
    }

    @NotNull
    protected Optional<JdbcConnection> getConnectionExternal() {
        var url = System.getenv(EXTERNAL_TEST_MARIADB_JDBC_URL);
        var host = System.getenv(EXTERNAL_TEST_MARIADB_HOST);
        var port = System.getenv(EXTERNAL_TEST_MARIADB_PORT);
        var user = System.getenv(EXTERNAL_TEST_MARIADB_USERNAME);
        var password = System.getenv(EXTERNAL_TEST_MARIADB_PASSWORD);
        var db = Optional.ofNullable(System.getenv(EXTERNAL_TEST_MARIADB_DATABASE)).orElse(DATABASE_NAME);

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
