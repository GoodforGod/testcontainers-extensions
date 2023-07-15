package io.goodforgod.testcontainers.extensions.jdbc;

import java.lang.annotation.Annotation;
import java.time.Duration;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.OracleContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.utility.DockerImageName;

final class TestcontainersOracleExtension extends AbstractTestcontainersJdbcExtension<OracleContainer> {

    private static final String PROTOCOL = "oracle:thin";
    private static final int ORACLE_PORT = 1521;

    private static final String EXTERNAL_TEST_ORACLE_JDBC_URL = "EXTERNAL_TEST_ORACLE_JDBC_URL";
    private static final String EXTERNAL_TEST_ORACLE_USERNAME = "EXTERNAL_TEST_ORACLE_USERNAME";
    private static final String EXTERNAL_TEST_ORACLE_PASSWORD = "EXTERNAL_TEST_ORACLE_PASSWORD";
    private static final String EXTERNAL_TEST_ORACLE_HOST = "EXTERNAL_TEST_ORACLE_HOST";
    private static final String EXTERNAL_TEST_ORACLE_PORT = "EXTERNAL_TEST_ORACLE_PORT";
    private static final String EXTERNAL_TEST_ORACLE_DATABASE = "EXTERNAL_TEST_ORACLE_DATABASE";

    @Override
    protected Class<OracleContainer> getContainerType() {
        return OracleContainer.class;
    }

    @Override
    protected Class<? extends Annotation> getContainerAnnotation() {
        return ContainerOracle.class;
    }

    @Override
    protected Class<? extends Annotation> getConnectionAnnotation() {
        return ContainerOracleConnection.class;
    }

    @NotNull
    protected OracleContainer getDefaultContainer(@NotNull String image) {
        var dockerImage = DockerImageName.parse(image)
                .asCompatibleSubstituteFor(DockerImageName.parse("gvenzl/oracle-xe"));

        var alias = "oracle-" + System.currentTimeMillis();
        return new OracleContainer(dockerImage)
                .withPassword("test")
                .withDatabaseName("oracle")
                .withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger(OracleContainer.class))
                        .withMdc("image", image)
                        .withMdc("alias", alias))
                .withNetworkAliases(alias)
                .withNetwork(Network.SHARED)
                .withStartupTimeout(Duration.ofMinutes(5));
    }

    @NotNull
    protected Optional<ContainerMetadata> findMetadata(@NotNull ExtensionContext context) {
        return findAnnotation(TestcontainersOracle.class, context)
                .map(a -> new ContainerMetadata(a.image(), a.mode(), a.migration()));
    }

    @NotNull
    protected JdbcConnection getConnectionForContainer(@NotNull OracleContainer container) {
        final String alias = container.getNetworkAliases().stream()
                .filter(a -> a.startsWith("oracle"))
                .findFirst()
                .or(() -> (container.getNetworkAliases().isEmpty())
                        ? Optional.empty()
                        : Optional.of(container.getNetworkAliases().get(container.getNetworkAliases().size() - 1)))
                .orElse(null);

        return JdbcConnectionImpl.forJDBC(container.getJdbcUrl(),
                container.getHost(),
                container.getMappedPort(ORACLE_PORT),
                alias,
                ORACLE_PORT,
                container.getDatabaseName(),
                container.getUsername(),
                container.getPassword());
    }

    @NotNull
    protected Optional<JdbcConnection> getConnectionExternal() {
        var url = System.getenv(EXTERNAL_TEST_ORACLE_JDBC_URL);
        var host = System.getenv(EXTERNAL_TEST_ORACLE_HOST);
        var port = System.getenv(EXTERNAL_TEST_ORACLE_PORT);
        var user = System.getenv(EXTERNAL_TEST_ORACLE_USERNAME);
        var password = System.getenv(EXTERNAL_TEST_ORACLE_PASSWORD);

        var db = Optional.ofNullable(System.getenv(EXTERNAL_TEST_ORACLE_DATABASE)).orElse("xepdb1");
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
