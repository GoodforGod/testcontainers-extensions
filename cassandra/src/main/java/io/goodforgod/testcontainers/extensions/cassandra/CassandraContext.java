package io.goodforgod.testcontainers.extensions.cassandra;

import io.goodforgod.testcontainers.extensions.ContainerContext;
import java.util.Optional;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.testcontainers.cassandra.CassandraContainer;

@Internal
final class CassandraContext implements ContainerContext<CassandraConnection> {

    static final Integer CQL_PORT = 9042;

    private static final String EXTERNAL_TEST_CASSANDRA_USERNAME = "EXTERNAL_TEST_CASSANDRA_USERNAME";
    private static final String EXTERNAL_TEST_CASSANDRA_PASSWORD = "EXTERNAL_TEST_CASSANDRA_PASSWORD";
    private static final String EXTERNAL_TEST_CASSANDRA_HOST = "EXTERNAL_TEST_CASSANDRA_HOST";
    private static final String EXTERNAL_TEST_CASSANDRA_PORT = "EXTERNAL_TEST_CASSANDRA_PORT";
    private static final String EXTERNAL_TEST_CASSANDRA_DATACENTER = "EXTERNAL_TEST_CASSANDRA_DATACENTER";
    private static final String EXTERNAL_TEST_CASSANDRA_KEYSPACE = "EXTERNAL_TEST_CASSANDRA_KEYSPACE";

    private volatile CassandraConnectionImpl connection;

    private final CassandraContainer container;

    CassandraContext(CassandraContainer container) {
        this.container = container;
    }

    @NotNull
    public CassandraConnection connection() {
        if (connection == null) {
            final Optional<CassandraConnection> connectionExternal = getConnectionExternal();
            if (connectionExternal.isEmpty() && !container.isRunning()) {
                throw new IllegalStateException("CassandraConnection can't be create for container that is not running");
            }

            final CassandraConnection jdbcConnection = connectionExternal.orElseGet(() -> {
                final String alias = container.getNetworkAliases().get(container.getNetworkAliases().size() - 1);
                return CassandraConnectionImpl.forContainer(container.getHost(),
                        container.getMappedPort(CQL_PORT),
                        alias,
                        CQL_PORT,
                        container.getLocalDatacenter(),
                        "cassandra",
                        container.getUsername(),
                        container.getPassword());
            });

            this.connection = (CassandraConnectionImpl) jdbcConnection;
        }

        return connection;
    }

    @Override
    public void start() {
        final Optional<CassandraConnection> connectionExternal = getConnectionExternal();
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
    private static Optional<CassandraConnection> getConnectionExternal() {
        var host = System.getenv(EXTERNAL_TEST_CASSANDRA_HOST);
        var port = System.getenv(EXTERNAL_TEST_CASSANDRA_PORT);
        var user = System.getenv(EXTERNAL_TEST_CASSANDRA_USERNAME);
        var password = System.getenv(EXTERNAL_TEST_CASSANDRA_PASSWORD);
        var dc = Optional.ofNullable(System.getenv(EXTERNAL_TEST_CASSANDRA_DATACENTER)).orElse("datacenter1");
        var keyspace = Optional.ofNullable(System.getenv(EXTERNAL_TEST_CASSANDRA_KEYSPACE)).orElse("cassandra");

        if (host != null && port != null) {
            return Optional.of(CassandraConnectionImpl.forExternal(host, Integer.parseInt(port), dc, keyspace, user, password));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public String toString() {
        return container.getDockerImageName();
    }
}
