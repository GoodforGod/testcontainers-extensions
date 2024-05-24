package io.goodforgod.testcontainers.extensions.jdbc;

import io.goodforgod.testcontainers.extensions.ContainerContext;
import java.util.Optional;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.testcontainers.clickhouse.ClickHouseContainer;

@Internal
final class ClickhouseContext implements ContainerContext<JdbcConnection> {

    private static final String PROTOCOL = "clickhouse";
    private static final int PORT = 8123;

    private static final String EXTERNAL_TEST_CLICKHOUSE_JDBC_URL = "EXTERNAL_TEST_CLICKHOUSE_JDBC_URL";
    private static final String EXTERNAL_TEST_CLICKHOUSE_USERNAME = "EXTERNAL_TEST_CLICKHOUSE_USERNAME";
    private static final String EXTERNAL_TEST_CLICKHOUSE_PASSWORD = "EXTERNAL_TEST_CLICKHOUSE_PASSWORD";
    private static final String EXTERNAL_TEST_CLICKHOUSE_HOST = "EXTERNAL_TEST_CLICKHOUSE_HOST";
    private static final String EXTERNAL_TEST_CLICKHOUSE_PORT = "EXTERNAL_TEST_CLICKHOUSE_PORT";
    private static final String EXTERNAL_TEST_CLICKHOUSE_DATABASE = "EXTERNAL_TEST_CLICKHOUSE_DATABASE";

    private volatile ClickhouseConnectionImpl connection;

    private final ClickHouseContainer container;

    ClickhouseContext(ClickHouseContainer container) {
        this.container = container;
    }

    @NotNull
    public JdbcConnection connection() {
        if (connection == null) {
            final Optional<JdbcConnection> connectionExternal = getConnectionExternal();
            if (connectionExternal.isEmpty() && !container.isRunning()) {
                throw new IllegalStateException("ClickhouseConnection can't be create for container that is not running");
            }

            final JdbcConnection containerConnection = connectionExternal.orElseGet(() -> {
                final String alias = container.getNetworkAliases().get(container.getNetworkAliases().size() - 1);
                return ClickhouseConnectionImpl.forJDBC(container.getJdbcUrl(),
                        container.getHost(),
                        container.getMappedPort(PORT),
                        alias,
                        PORT,
                        container.getDatabaseName(),
                        container.getUsername(),
                        container.getPassword());
            });

            this.connection = (ClickhouseConnectionImpl) containerConnection;
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
        var url = System.getenv(EXTERNAL_TEST_CLICKHOUSE_JDBC_URL);
        var host = System.getenv(EXTERNAL_TEST_CLICKHOUSE_HOST);
        var port = System.getenv(EXTERNAL_TEST_CLICKHOUSE_PORT);
        var user = System.getenv(EXTERNAL_TEST_CLICKHOUSE_USERNAME);
        var password = System.getenv(EXTERNAL_TEST_CLICKHOUSE_PASSWORD);

        var db = Optional.ofNullable(System.getenv(EXTERNAL_TEST_CLICKHOUSE_DATABASE)).orElse("default");
        if (url != null) {
            if (host != null && port != null) {
                return Optional.of(ClickhouseConnectionImpl.forJDBC(url, host, Integer.parseInt(port), null, null, db, user, password));
            } else {
                return Optional.of(ClickhouseConnectionImpl.forExternal(url, user, password));
            }
        } else if (host != null && port != null) {
            return Optional.of(ClickhouseConnectionImpl.forProtocol(PROTOCOL, host, Integer.parseInt(port), db, user, password));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public String toString() {
        return container.getDockerImageName();
    }
}
