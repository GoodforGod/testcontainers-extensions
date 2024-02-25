package io.goodforgod.testcontainers.extensions.jdbc;

import io.goodforgod.testcontainers.extensions.ContainerContext;
import java.util.Optional;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.testcontainers.containers.OracleContainer;

@Internal
final class OracleContext implements ContainerContext<JdbcConnection> {

    private static final String PROTOCOL = "oracle:thin";
    private static final int ORACLE_PORT = 1521;

    private static final String EXTERNAL_TEST_ORACLE_JDBC_URL = "EXTERNAL_TEST_ORACLE_JDBC_URL";
    private static final String EXTERNAL_TEST_ORACLE_USERNAME = "EXTERNAL_TEST_ORACLE_USERNAME";
    private static final String EXTERNAL_TEST_ORACLE_PASSWORD = "EXTERNAL_TEST_ORACLE_PASSWORD";
    private static final String EXTERNAL_TEST_ORACLE_HOST = "EXTERNAL_TEST_ORACLE_HOST";
    private static final String EXTERNAL_TEST_ORACLE_PORT = "EXTERNAL_TEST_ORACLE_PORT";
    private static final String EXTERNAL_TEST_ORACLE_DATABASE = "EXTERNAL_TEST_ORACLE_DATABASE";

    private volatile JdbcConnectionImpl connection;

    private final OracleContainer container;

    OracleContext(OracleContainer container) {
        this.container = container;
    }

    @NotNull
    public JdbcConnection connection() {
        if (connection == null) {
            final Optional<JdbcConnection> connectionExternal = getConnectionExternal();
            if (connectionExternal.isEmpty() && !container.isRunning()) {
                throw new IllegalStateException("MysqlConnection can't be create for container that is not running");
            }

            final JdbcConnection containerConnection = connectionExternal.orElseGet(() -> {
                final String alias = container.getNetworkAliases().get(container.getNetworkAliases().size() - 1);
                return JdbcConnectionImpl.forJDBC(container.getJdbcUrl(),
                        container.getHost(),
                        container.getMappedPort(ORACLE_PORT),
                        alias,
                        ORACLE_PORT,
                        container.getDatabaseName(),
                        container.getUsername(),
                        container.getPassword());
            });

            this.connection = (JdbcConnectionImpl) containerConnection;
        }

        return connection;
    }

    @Override
    public void start() {
        final Optional<JdbcConnection> connectionExternal = getConnectionExternal();
        if (connectionExternal.isEmpty()) {
            container.start();
        }
    }

    @Override
    public void stop() {
        if (connection != null) {
            connection.stop();
            connection = null;
        }
        container.stop();
    }

    @NotNull
    private static Optional<JdbcConnection> getConnectionExternal() {
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

    @Override
    public String toString() {
        return container.getDockerImageName();
    }
}
