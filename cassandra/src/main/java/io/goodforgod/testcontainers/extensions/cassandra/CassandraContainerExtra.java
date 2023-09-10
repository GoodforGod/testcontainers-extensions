package io.goodforgod.testcontainers.extensions.cassandra;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.CassandraContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

public class CassandraContainerExtra<SELF extends CassandraContainerExtra<SELF>> extends CassandraContainer<SELF> {

    private static final String EXTERNAL_TEST_CASSANDRA_USERNAME = "EXTERNAL_TEST_CASSANDRA_USERNAME";
    private static final String EXTERNAL_TEST_CASSANDRA_PASSWORD = "EXTERNAL_TEST_CASSANDRA_PASSWORD";
    private static final String EXTERNAL_TEST_CASSANDRA_HOST = "EXTERNAL_TEST_CASSANDRA_HOST";
    private static final String EXTERNAL_TEST_CASSANDRA_PORT = "EXTERNAL_TEST_CASSANDRA_PORT";
    private static final String EXTERNAL_TEST_CASSANDRA_DATACENTER = "EXTERNAL_TEST_CASSANDRA_DATACENTER";

    private volatile CassandraConnectionImpl connection;

    public CassandraContainerExtra(String dockerImageName) {
        this(DockerImageName.parse(dockerImageName));
    }

    public CassandraContainerExtra(DockerImageName dockerImageName) {
        super(dockerImageName);

        final String alias = "cassandra-" + System.currentTimeMillis();
        this.withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger(CassandraContainer.class))
                .withMdc("image", dockerImageName.asCanonicalNameString())
                .withMdc("alias", alias));
        this.waitingFor(Wait.forListeningPort());
        this.withStartupTimeout(Duration.ofMinutes(5));

        this.setNetworkAliases(new ArrayList<>(List.of(alias)));
    }

    @NotNull
    public CassandraConnection connection() {
        if (connection == null) {
            final Optional<CassandraConnection> connectionExternal = getConnectionExternal();
            if (connectionExternal.isEmpty() && !isRunning()) {
                throw new IllegalStateException("CassandraConnection can't be create for container that is not running");
            }

            final CassandraConnection jdbcConnection = connectionExternal.orElseGet(() -> {
                final String alias = getNetworkAliases().get(getNetworkAliases().size() - 1);
                return CassandraConnectionImpl.forContainer(getHost(),
                        getMappedPort(CassandraContainer.CQL_PORT),
                        alias,
                        CassandraContainer.CQL_PORT,
                        getLocalDatacenter(),
                        getUsername(),
                        getPassword());
            });

            this.connection = (CassandraConnectionImpl) jdbcConnection;
        }

        return connection;
    }

    @Override
    public void start() {
        final Optional<CassandraConnection> connectionExternal = getConnectionExternal();
        if (connectionExternal.isEmpty()) {
            super.start();
        }
    }

    @Override
    public void stop() {
        connection.close();
        connection = null;
        super.stop();
    }

    @NotNull
    private static Optional<CassandraConnection> getConnectionExternal() {
        var host = System.getenv(EXTERNAL_TEST_CASSANDRA_HOST);
        var port = System.getenv(EXTERNAL_TEST_CASSANDRA_PORT);
        var user = System.getenv(EXTERNAL_TEST_CASSANDRA_USERNAME);
        var password = System.getenv(EXTERNAL_TEST_CASSANDRA_PASSWORD);
        var dc = Optional.ofNullable(System.getenv(EXTERNAL_TEST_CASSANDRA_DATACENTER)).orElse("datacenter1");

        if (host != null && port != null) {
            return Optional.of(CassandraConnectionImpl.forExternal(host, Integer.parseInt(port), dc, user, password));
        } else
            return Optional.empty();
    }
}
