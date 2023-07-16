package io.goodforgod.testcontainers.extensions.jdbc;

import java.lang.annotation.Annotation;
import java.time.Duration;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

final class TestcontainersMysqlExtension extends AbstractTestcontainersJdbcExtension<MySQLContainer<?>> {

    private static final String PROTOCOL = "mysql";
    private static final String DATABASE_NAME = "test";

    private static final String EXTERNAL_TEST_MYSQL_JDBC_URL = "EXTERNAL_TEST_MYSQL_JDBC_URL";
    private static final String EXTERNAL_TEST_MYSQL_USERNAME = "EXTERNAL_TEST_MYSQL_USERNAME";
    private static final String EXTERNAL_TEST_MYSQL_PASSWORD = "EXTERNAL_TEST_MYSQL_PASSWORD";
    private static final String EXTERNAL_TEST_MYSQL_HOST = "EXTERNAL_TEST_MYSQL_HOST";
    private static final String EXTERNAL_TEST_MYSQL_PORT = "EXTERNAL_TEST_MYSQL_PORT";
    private static final String EXTERNAL_TEST_MYSQL_DATABASE = "EXTERNAL_TEST_MYSQL_DATABASE";

    @SuppressWarnings("unchecked")
    @Override
    protected Class<MySQLContainer<?>> getContainerType() {
        return (Class<MySQLContainer<?>>) ((Class<?>) MySQLContainer.class);
    }

    @Override
    protected Class<? extends Annotation> getContainerAnnotation() {
        return ContainerMysql.class;
    }

    @Override
    protected Class<? extends Annotation> getConnectionAnnotation() {
        return ContainerMysqlConnection.class;
    }

    @NotNull
    protected MySQLContainer<?> getDefaultContainer(@NotNull String image) {
        var dockerImage = DockerImageName.parse(image)
                .asCompatibleSubstituteFor(DockerImageName.parse(MySQLContainer.NAME));

        var alias = "mysql-" + System.currentTimeMillis();
        return new MySQLContainer<>(dockerImage)
                .withDatabaseName(DATABASE_NAME)
                .withUsername("mysql")
                .withPassword("mysql")
                .withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger(MySQLContainer.class))
                        .withMdc("image", image)
                        .withMdc("alias", alias))
                .withNetworkAliases(alias)
                .withNetwork(Network.SHARED)
                .waitingFor(Wait.forHealthcheck())
                .withStartupTimeout(Duration.ofMinutes(5));
    }

    @NotNull
    protected Optional<ContainerMetadata> findMetadata(@NotNull ExtensionContext context) {
        return findAnnotation(TestcontainersMysql.class, context)
                .map(a -> new ContainerMetadata(a.image(), a.mode(), a.migration()));
    }

    @NotNull
    protected JdbcConnection getConnectionForContainer(@NotNull MySQLContainer<?> container) {
        final String alias = container.getNetworkAliases().stream()
                .filter(a -> a.startsWith("mysql"))
                .findFirst()
                .or(() -> (container.getNetworkAliases().isEmpty())
                        ? Optional.empty()
                        : Optional.of(container.getNetworkAliases().get(container.getNetworkAliases().size() - 1)))
                .orElse(null);

        return JdbcConnectionImpl.forJDBC(container.getJdbcUrl(),
                container.getHost(),
                container.getMappedPort(MySQLContainer.MYSQL_PORT),
                alias,
                MySQLContainer.MYSQL_PORT,
                container.getDatabaseName(),
                container.getUsername(),
                container.getPassword());
    }

    @NotNull
    protected Optional<JdbcConnection> getConnectionExternal() {
        var url = System.getenv(EXTERNAL_TEST_MYSQL_JDBC_URL);
        var host = System.getenv(EXTERNAL_TEST_MYSQL_HOST);
        var port = System.getenv(EXTERNAL_TEST_MYSQL_PORT);
        var user = System.getenv(EXTERNAL_TEST_MYSQL_USERNAME);
        var password = System.getenv(EXTERNAL_TEST_MYSQL_PASSWORD);

        var db = Optional.ofNullable(System.getenv(EXTERNAL_TEST_MYSQL_DATABASE)).orElse(DATABASE_NAME);
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
