package io.goodforgod.testcontainers.extensions.jdbc;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.CockroachContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.utility.DockerImageName;

public class CockroachContainerExtra extends CockroachContainer {

    private static final String PROTOCOL = "postgresql";
    private static final int PORT = 26257;

    private static final String EXTERNAL_TEST_COCKROACHDB_JDBC_URL = "EXTERNAL_TEST_COCKROACHDB_JDBC_URL";
    private static final String EXTERNAL_TEST_COCKROACHDB_USERNAME = "EXTERNAL_TEST_COCKROACHDB_USERNAME";
    private static final String EXTERNAL_TEST_COCKROACHDB_PASSWORD = "EXTERNAL_TEST_COCKROACHDB_PASSWORD";
    private static final String EXTERNAL_TEST_COCKROACHDB_HOST = "EXTERNAL_TEST_COCKROACHDB_HOST";
    private static final String EXTERNAL_TEST_COCKROACHDB_PORT = "EXTERNAL_TEST_COCKROACHDB_PORT";
    private static final String EXTERNAL_TEST_COCKROACHDB_DATABASE = "EXTERNAL_TEST_COCKROACHDB_DATABASE";

    private volatile JdbcConnectionImpl connection;
    private volatile FlywayJdbcMigrationEngine flywayJdbcMigrationEngine;
    private volatile LiquibaseJdbcMigrationEngine liquibaseJdbcMigrationEngine;

    public CockroachContainerExtra(String dockerImageName) {
        this(DockerImageName.parse(dockerImageName));
    }

    public CockroachContainerExtra(DockerImageName dockerImageName) {
        super(dockerImageName);

        final String alias = "cockroachdb-" + System.currentTimeMillis();
        this.withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger(CockroachContainerExtra.class))
                .withMdc("image", dockerImageName.asCanonicalNameString())
                .withMdc("alias", alias));
        this.withStartupTimeout(Duration.ofMinutes(5));

        this.setNetworkAliases(new ArrayList<>(List.of(alias)));
    }

    @Internal
    JdbcMigrationEngine getMigrationEngine(@NotNull Migration.Engines engine) {
        if (engine == Migration.Engines.FLYWAY) {
            if (flywayJdbcMigrationEngine == null) {
                this.flywayJdbcMigrationEngine = new FlywayJdbcMigrationEngine(connection());
            }
            return this.flywayJdbcMigrationEngine;
        } else if (engine == Migration.Engines.LIQUIBASE) {
            if (liquibaseJdbcMigrationEngine == null) {
                this.liquibaseJdbcMigrationEngine = new LiquibaseJdbcMigrationEngine(connection());
            }
            return this.liquibaseJdbcMigrationEngine;
        } else {
            throw new UnsupportedOperationException("Unsupported engine: " + engine);
        }
    }

    @NotNull
    public JdbcConnection connection() {
        if (connection == null) {
            final Optional<JdbcConnection> connectionExternal = getConnectionExternal();
            if (connectionExternal.isEmpty() && !isRunning()) {
                throw new IllegalStateException("MariadbConnection can't be create for container that is not running");
            }

            final JdbcConnection jdbcConnection = connectionExternal.orElseGet(() -> {
                final String alias = getNetworkAliases().get(getNetworkAliases().size() - 1);
                return JdbcConnectionImpl.forJDBC(getJdbcUrl(),
                        getHost(),
                        getMappedPort(PORT),
                        alias,
                        PORT,
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
        if (flywayJdbcMigrationEngine != null) {
            flywayJdbcMigrationEngine.close();
            flywayJdbcMigrationEngine = null;
        }
        if (liquibaseJdbcMigrationEngine != null) {
            liquibaseJdbcMigrationEngine.close();
            liquibaseJdbcMigrationEngine = null;
        }
        if (connection != null) {
            connection.close();
            connection = null;
        }
        super.stop();
    }

    @NotNull
    private static Optional<JdbcConnection> getConnectionExternal() {
        var url = System.getenv(EXTERNAL_TEST_COCKROACHDB_JDBC_URL);
        var host = System.getenv(EXTERNAL_TEST_COCKROACHDB_HOST);
        var port = System.getenv(EXTERNAL_TEST_COCKROACHDB_PORT);
        var user = System.getenv(EXTERNAL_TEST_COCKROACHDB_USERNAME);
        var password = System.getenv(EXTERNAL_TEST_COCKROACHDB_PASSWORD);

        var db = Optional.ofNullable(System.getenv(EXTERNAL_TEST_COCKROACHDB_DATABASE)).orElse("cockroachdb");
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
