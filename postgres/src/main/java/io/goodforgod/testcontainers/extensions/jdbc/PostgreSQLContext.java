package io.goodforgod.testcontainers.extensions.jdbc;

import io.goodforgod.testcontainers.extensions.ContainerContext;
import java.util.Optional;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.testcontainers.containers.PostgreSQLContainer;

@Internal
final class PostgreSQLContext implements ContainerContext<JdbcConnection> {

    private static final String PROTOCOL = "postgresql";

    private static final String EXTERNAL_TEST_POSTGRES_JDBC_URL = "EXTERNAL_TEST_POSTGRES_JDBC_URL";
    private static final String EXTERNAL_TEST_POSTGRES_USERNAME = "EXTERNAL_TEST_POSTGRES_USERNAME";
    private static final String EXTERNAL_TEST_POSTGRES_PASSWORD = "EXTERNAL_TEST_POSTGRES_PASSWORD";
    private static final String EXTERNAL_TEST_POSTGRES_HOST = "EXTERNAL_TEST_POSTGRES_HOST";
    private static final String EXTERNAL_TEST_POSTGRES_PORT = "EXTERNAL_TEST_POSTGRES_PORT";
    private static final String EXTERNAL_TEST_POSTGRES_DATABASE = "EXTERNAL_TEST_POSTGRES_DATABASE";

    private volatile JdbcConnectionImpl connection;

    private final PostgreSQLContainer<?> container;

    PostgreSQLContext(PostgreSQLContainer<?> container) {
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
                        container.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT),
                        alias,
                        PostgreSQLContainer.POSTGRESQL_PORT,
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
        var url = System.getenv(EXTERNAL_TEST_POSTGRES_JDBC_URL);
        var host = System.getenv(EXTERNAL_TEST_POSTGRES_HOST);
        var port = System.getenv(EXTERNAL_TEST_POSTGRES_PORT);
        var user = System.getenv(EXTERNAL_TEST_POSTGRES_USERNAME);
        var password = System.getenv(EXTERNAL_TEST_POSTGRES_PASSWORD);
        var db = Optional.ofNullable(System.getenv(EXTERNAL_TEST_POSTGRES_DATABASE)).orElse("postgres");

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
