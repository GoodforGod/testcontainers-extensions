package io.goodforgod.testcontainers.extensions.scylla;

import static io.goodforgod.testcontainers.extensions.scylla.ScyllaConnectionImpl.KEYSPACE;

import io.goodforgod.testcontainers.extensions.ContainerContext;
import java.util.Optional;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.testcontainers.scylladb.ScyllaDBContainer;

@Internal
final class ScyllaContext implements ContainerContext<ScyllaConnection> {

    private static final String EXTERNAL_TEST_SCYLLA_USERNAME = "EXTERNAL_TEST_SCYLLA_USERNAME";
    private static final String EXTERNAL_TEST_SCYLLA_PASSWORD = "EXTERNAL_TEST_SCYLLA_PASSWORD";
    private static final String EXTERNAL_TEST_SCYLLA_HOST = "EXTERNAL_TEST_SCYLLA_HOST";
    private static final String EXTERNAL_TEST_SCYLLA_PORT = "EXTERNAL_TEST_SCYLLA_PORT";
    private static final String EXTERNAL_TEST_SCYLLA_DATACENTER = "EXTERNAL_TEST_SCYLLA_DATACENTER";
    private static final String EXTERNAL_TEST_SCYLLA_KEYSPACE = "EXTERNAL_TEST_SCYLLA_KEYSPACE";

    private volatile ScyllaConnectionImpl connection;

    private final ScyllaDBContainer container;

    ScyllaContext(ScyllaDBContainer container) {
        this.container = container;
    }

    @NotNull
    public ScyllaConnection connection() {
        if (connection == null) {
            final Optional<ScyllaConnection> connectionExternal = getConnectionExternal();
            if (connectionExternal.isEmpty() && !container.isRunning()) {
                throw new IllegalStateException("ScyllaConnection can't be create for container that is not running");
            }

            final ScyllaConnection jdbcConnection = connectionExternal.orElseGet(() -> {
                final String alias = container.getNetworkAliases().get(container.getNetworkAliases().size() - 1);
                return ScyllaConnectionImpl.forContainer(container.getHost(),
                        container.getMappedPort(ScyllaConnectionImpl.CQL_PORT),
                        alias,
                        ScyllaConnectionImpl.CQL_PORT,
                        ScyllaConnectionImpl.LOCAL_DATACENTER,
                        KEYSPACE,
                        ScyllaConnectionImpl.USERNAME,
                        ScyllaConnectionImpl.PASSWORD);
            });

            this.connection = (ScyllaConnectionImpl) jdbcConnection;
        }

        return connection;
    }

    @Override
    public void start() {
        final Optional<ScyllaConnection> connectionExternal = getConnectionExternal();
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
    private static Optional<ScyllaConnection> getConnectionExternal() {
        var host = System.getenv(EXTERNAL_TEST_SCYLLA_HOST);
        var port = System.getenv(EXTERNAL_TEST_SCYLLA_PORT);
        var user = System.getenv(EXTERNAL_TEST_SCYLLA_USERNAME);
        var password = System.getenv(EXTERNAL_TEST_SCYLLA_PASSWORD);
        var dc = Optional.ofNullable(System.getenv(EXTERNAL_TEST_SCYLLA_DATACENTER))
                .orElse(ScyllaConnectionImpl.LOCAL_DATACENTER);
        var keyspace = Optional.ofNullable(System.getenv(EXTERNAL_TEST_SCYLLA_KEYSPACE)).orElse(ScyllaConnectionImpl.KEYSPACE);

        if (host != null && port != null) {
            return Optional.of(ScyllaConnectionImpl.forExternal(host, Integer.parseInt(port), dc, keyspace, user, password));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public String toString() {
        return container.getDockerImageName();
    }
}
