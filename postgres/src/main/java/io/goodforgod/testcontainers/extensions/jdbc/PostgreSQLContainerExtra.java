package io.goodforgod.testcontainers.extensions.jdbc;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.utility.DockerImageName;

public class PostgreSQLContainerExtra<SELF extends PostgreSQLContainerExtra<SELF>> extends PostgreSQLContainer<SELF> {

    private static final String PROTOCOL = "postgresql";

    private static final String EXTERNAL_TEST_POSTGRES_JDBC_URL = "EXTERNAL_TEST_POSTGRES_JDBC_URL";
    private static final String EXTERNAL_TEST_POSTGRES_USERNAME = "EXTERNAL_TEST_POSTGRES_USERNAME";
    private static final String EXTERNAL_TEST_POSTGRES_PASSWORD = "EXTERNAL_TEST_POSTGRES_PASSWORD";
    private static final String EXTERNAL_TEST_POSTGRES_HOST = "EXTERNAL_TEST_POSTGRES_HOST";
    private static final String EXTERNAL_TEST_POSTGRES_PORT = "EXTERNAL_TEST_POSTGRES_PORT";
    private static final String EXTERNAL_TEST_POSTGRES_DATABASE = "EXTERNAL_TEST_POSTGRES_DATABASE";

    private volatile JdbcConnectionImpl connection;

    public PostgreSQLContainerExtra(String dockerImageName) {
        this(DockerImageName.parse(dockerImageName));
    }

    public PostgreSQLContainerExtra(DockerImageName dockerImageName) {
        super(dockerImageName);

        final String alias = "postgres-" + System.currentTimeMillis();
        this.withDatabaseName("postgres");
        this.withUsername("postgres");
        this.withPassword("postgres");
        this.withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger(PostgreSQLContainerExtra.class))
                .withMdc("image", dockerImageName.asCanonicalNameString())
                .withMdc("alias", alias));
        this.withStartupTimeout(Duration.ofMinutes(5));

        setNetworkAliases(new ArrayList<>(List.of(alias)));
    }

    @NotNull
    public JdbcConnection connection() {
        if (connection == null) {
            final Optional<JdbcConnection> connectionExternal = getConnectionExternal();
            if (connectionExternal.isEmpty() && !isRunning()) {
                throw new IllegalStateException("PostgresConnection can't be create for container that is not running");
            }

            final JdbcConnection jdbcConnection = connectionExternal.orElseGet(() -> {
                final String alias = getNetworkAliases().get(getNetworkAliases().size() - 1);
                return JdbcConnectionImpl.forJDBC(getJdbcUrl(),
                        getHost(),
                        getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT),
                        alias,
                        PostgreSQLContainer.POSTGRESQL_PORT,
                        getDatabaseName(),
                        getUsername(),
                        getPassword());
            });

            this.connection = (JdbcConnectionImpl) jdbcConnection;
        }

        return connection;
    }

    @Override
    public void start() {
        final Optional<JdbcConnection> connectionExternal = getConnectionExternal();
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
}
