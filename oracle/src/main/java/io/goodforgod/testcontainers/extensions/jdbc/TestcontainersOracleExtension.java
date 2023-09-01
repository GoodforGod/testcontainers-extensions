package io.goodforgod.testcontainers.extensions.jdbc;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.OracleContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.utility.DockerImageName;

import java.lang.annotation.Annotation;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

final class TestcontainersOracleExtension extends AbstractTestcontainersJdbcExtension<OracleContainer, OracleMetadata> {

    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace
            .create(TestcontainersOracleExtension.class);

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

    @Override
    protected OracleContainer getContainerDefault(OracleMetadata metadata) {
        var dockerImage = DockerImageName.parse(metadata.image())
                .asCompatibleSubstituteFor(DockerImageName.parse("gvenzl/oracle-xe"));

        var container = new OracleContainer(dockerImage)
                .withPassword("test")
                .withDatabaseName("oracle")
                .withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger(OracleContainer.class))
                        .withMdc("image", metadata.image())
                        .withMdc("alias", metadata.networkAliasOrDefault()))
                .withStartupTimeout(Duration.ofMinutes(5));

        container.setNetworkAliases(new ArrayList<>(List.of(metadata.networkAliasOrDefault())));
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
    protected Optional<OracleMetadata> findMetadata(@NotNull ExtensionContext context) {
        return findAnnotation(TestcontainersOracle.class, context)
                .map(a -> new OracleMetadata(a.network().shared(), a.network().alias(), a.image(), a.mode(), a.migration()));
    }

    @NotNull
    protected JdbcConnection getConnectionForContainer(OracleMetadata metadata, @NotNull OracleContainer container) {
        final String alias = container.getNetworkAliases().stream()
                .filter(a -> a.equals(metadata.networkAliasOrDefault()))
                .findFirst()
                .or(() -> container.getNetworkAliases().stream().findFirst())
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
