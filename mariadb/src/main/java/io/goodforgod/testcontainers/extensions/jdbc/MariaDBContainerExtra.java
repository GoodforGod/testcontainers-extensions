package io.goodforgod.testcontainers.extensions.jdbc;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

public class MariaDBContainerExtra<SELF extends MariaDBContainerExtra<SELF>> extends MariaDBContainer<SELF> {

    private static final String PROTOCOL = "mariadb";
    private static final String DATABASE_NAME = "mariadb";
    private static final Integer MARIADB_PORT = 3306;

    private static final String EXTERNAL_TEST_MARIADB_JDBC_URL = "EXTERNAL_TEST_MARIADB_JDBC_URL";
    private static final String EXTERNAL_TEST_MARIADB_USERNAME = "EXTERNAL_TEST_MARIADB_USERNAME";
    private static final String EXTERNAL_TEST_MARIADB_PASSWORD = "EXTERNAL_TEST_MARIADB_PASSWORD";
    private static final String EXTERNAL_TEST_MARIADB_HOST = "EXTERNAL_TEST_MARIADB_HOST";
    private static final String EXTERNAL_TEST_MARIADB_PORT = "EXTERNAL_TEST_MARIADB_PORT";
    private static final String EXTERNAL_TEST_MARIADB_DATABASE = "EXTERNAL_TEST_MARIADB_DATABASE";

    private volatile JdbcConnectionImpl connection;
    private volatile FlywayJdbcMigrationEngine flywayJdbcMigrationEngine;
    private volatile LiquibaseJdbcMigrationEngine liquibaseJdbcMigrationEngine;

    public MariaDBContainerExtra(String dockerImageName) {
        this(DockerImageName.parse(dockerImageName));
    }

    public MariaDBContainerExtra(DockerImageName dockerImageName) {
        super(dockerImageName);

        final String alias = "mariadb-" + System.currentTimeMillis();

        this.withDatabaseName(DATABASE_NAME);
        this.withUsername("mariadb");
        this.withPassword("mariadb");
        this.withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger(MariaDBContainerExtra.class))
                .withMdc("image", dockerImageName.asCanonicalNameString())
                .withMdc("alias", alias));
        this.waitingFor(Wait.forListeningPort());
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
                        getMappedPort(MARIADB_PORT),
                        alias,
                        MARIADB_PORT,
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
        var url = System.getenv(EXTERNAL_TEST_MARIADB_JDBC_URL);
        var host = System.getenv(EXTERNAL_TEST_MARIADB_HOST);
        var port = System.getenv(EXTERNAL_TEST_MARIADB_PORT);
        var user = System.getenv(EXTERNAL_TEST_MARIADB_USERNAME);
        var password = System.getenv(EXTERNAL_TEST_MARIADB_PASSWORD);
        var db = Optional.ofNullable(System.getenv(EXTERNAL_TEST_MARIADB_DATABASE)).orElse(DATABASE_NAME);

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
