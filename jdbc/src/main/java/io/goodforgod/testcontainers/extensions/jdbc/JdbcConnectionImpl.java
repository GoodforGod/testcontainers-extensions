package io.goodforgod.testcontainers.extensions.jdbc;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Internal
class JdbcConnectionImpl implements JdbcConnection {

    static final class ParamsImpl implements JdbcConnection.Params {

        private final String jdbcUrl;
        private final String host;
        private final int port;
        private final String database;
        private final String username;
        private final String password;

        ParamsImpl(String jdbcUrl, String host, int port, String database, String username, String password) {
            this.jdbcUrl = jdbcUrl;
            this.host = host;
            this.port = port;
            this.database = database;
            this.username = username;
            this.password = password;
        }

        @Override
        public @NotNull String jdbcUrl() {
            return jdbcUrl;
        }

        @Override
        public @NotNull String host() {
            return host;
        }

        @Override
        public int port() {
            return port;
        }

        @Override
        public @NotNull String database() {
            return database;
        }

        @Override
        public String username() {
            return username;
        }

        @Override
        public String password() {
            return password;
        }

        @Override
        public String toString() {
            return jdbcUrl;
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(JdbcConnection.class);

    private final Params params;
    private final Params network;

    private volatile FlywayJdbcMigrationEngine flywayJdbcMigrationEngine;
    private volatile LiquibaseJdbcMigrationEngine liquibaseJdbcMigrationEngine;

    private volatile boolean isClosed = false;
    private volatile Connection connection;

    JdbcConnectionImpl(Params params, Params network) {
        this.params = params;
        this.network = network;
    }

    static JdbcConnection forProtocol(String driverProtocol,
                                      String host,
                                      int port,
                                      String database,
                                      String username,
                                      String password) {
        var jdbcUrl = String.format("jdbc:%s://%s:%d/%s", driverProtocol, host, port, database);
        var params = new ParamsImpl(jdbcUrl, host, port, database, username, password);
        return new JdbcConnectionImpl(params, null);
    }

    static JdbcConnection forJDBC(String jdbcUrl,
                                  String host,
                                  int port,
                                  String hostInNetwork,
                                  Integer portInNetwork,
                                  String database,
                                  String username,
                                  String password) {
        var params = new ParamsImpl(jdbcUrl, host, port, database, username, password);
        final Params network;
        if (hostInNetwork == null) {
            network = null;
        } else {
            var jdbcUrlInNetwork = jdbcUrl.replace(host + ":" + port, hostInNetwork + ":" + portInNetwork);
            network = new ParamsImpl(jdbcUrlInNetwork, hostInNetwork, portInNetwork, database, username, password);
        }

        return new JdbcConnectionImpl(params, network);
    }

    static JdbcConnection forExternal(String jdbcUrl, String username, String password) {
        final URI uri = URI.create(jdbcUrl);
        var host = uri.getHost();
        var port = uri.getPort();

        final int dbSeparator = uri.getPath().indexOf(';');
        final String database = (dbSeparator == -1)
                ? uri.getPath()
                : uri.getPath().substring(0, dbSeparator);

        var params = new ParamsImpl(jdbcUrl, host, port, database, username, password);
        return new JdbcConnectionImpl(params, null);
    }

    @Override
    public @NotNull JdbcMigrationEngine migrationEngine(Migration.@NotNull Engines engine) {
        if (engine == Migration.Engines.FLYWAY) {
            if (flywayJdbcMigrationEngine == null) {
                this.flywayJdbcMigrationEngine = new FlywayJdbcMigrationEngine(this);
            }
            return this.flywayJdbcMigrationEngine;
        } else if (engine == Migration.Engines.LIQUIBASE) {
            if (liquibaseJdbcMigrationEngine == null) {
                this.liquibaseJdbcMigrationEngine = new LiquibaseJdbcMigrationEngine(this);
            }
            return this.liquibaseJdbcMigrationEngine;
        } else {
            throw new UnsupportedOperationException("Unsupported engine: " + engine);
        }
    }

    @Override
    public @NotNull Params params() {
        return params;
    }

    @Override
    public @NotNull Optional<Params> paramsInNetwork() {
        return Optional.ofNullable(network);
    }

    @NotNull
    @Override
    public Connection get() {
        if (isClosed) {
            throw new IllegalStateException("JdbcConnection was closed");
        }

        try {
            if (connection == null) {
                connection = openConnection();
            } else if (connection.isClosed()) {
                connection = openConnection();
            }

            return connection;
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    @NotNull
    private Connection openConnection() {
        try {
            logger.debug("Opening SQL connection...");
            return DriverManager.getConnection(params.jdbcUrl(), params.username(), params.username());
        } catch (SQLException e) {
            throw new JdbcConnectionException(e);
        }
    }

    @Override
    public void execute(@Language("SQL") @NotNull String sql) {
        logger.debug("Executing SQL:\n{}", sql);
        try (var connection = get();
                var stmt = connection.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            throw new JdbcConnectionException(e);
        }
    }

    @Override
    public void executeFromResources(@NotNull String pathToResource) {
        logger.debug("Loading file from resources with path: {}", pathToResource);
        var resourceAsString = loadStringFromResources(pathToResource)
                .orElseThrow(() -> new IllegalArgumentException("Couldn't find resource with path: " + pathToResource));
        execute(resourceAsString);
    }

    private Optional<String> loadStringFromResources(String path) {
        try {
            try (var resource = JdbcConnectionImpl.class.getClassLoader().getResourceAsStream(path)) {
                if (resource == null) {
                    return Optional.empty();
                } else {
                    return Optional.of(new String(resource.readAllBytes(), StandardCharsets.UTF_8));
                }
            }
        } catch (Exception e) {
            logger.warn("Failed loading '{}' due to: {}", path, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public int count(@NotNull String tableName) {
        return queryOne("SELECT COUNT(*) FROM " + tableName, rs -> rs.getInt(1)).orElse(0);
    }

    @Override
    public void assertCountsNone(@NotNull String tableName) {
        assertCountsEquals(0, tableName);
    }

    @Override
    public void assertCountsAtLeast(int expectedAtLeast, @NotNull String tableName) {
        final int count = count(tableName);
        if (count < expectedAtLeast) {
            Assertions.assertEquals(expectedAtLeast, count,
                    String.format("Expected to count in '%s' table at least %s rows but received %s",
                            tableName, expectedAtLeast, count));
        }
    }

    @Override
    public void assertCountsEquals(int expected, @NotNull String tableName) {
        final int count = count(tableName);
        Assertions.assertEquals(expected, count, String.format("Expected to count in '%s' table %s rows but received %s",
                tableName, expected, count));
    }

    @Override
    public <T, E extends Throwable> Optional<T> queryOne(@Language("SQL") @NotNull String sql,
                                                         @NotNull ResultSetMapper<T, E> extractor)
            throws E {
        logger.debug("Executing SQL:\n{}", sql);
        try (var connection = get();
                var stmt = connection.prepareStatement(sql);
                var rs = stmt.executeQuery()) {
            return (rs.next())
                    ? Optional.ofNullable(extractor.apply(rs))
                    : Optional.empty();
        } catch (SQLException e) {
            throw new JdbcConnectionException(e);
        }
    }

    @Override
    public <T, E extends Throwable> List<T> queryMany(@Language("SQL") @NotNull String sql,
                                                      @NotNull ResultSetMapper<T, E> extractor)
            throws E {
        logger.debug("Executing SQL:\n{}", sql);
        try (var connection = get();
                var stmt = connection.prepareStatement(sql);
                var rs = stmt.executeQuery()) {
            final List<T> result = new ArrayList<>();
            while (rs.next()) {
                result.add(extractor.apply(rs));
            }
            return result;
        } catch (SQLException e) {
            throw new JdbcConnectionException(e);
        }
    }

    @FunctionalInterface
    interface QueryAssert {

        void accept(@NotNull ResultSet rs) throws SQLException;
    }

    private void assertQuery(@Language("SQL") String sql, QueryAssert consumer) {
        logger.debug("Executing SQL:\n{}", sql);
        try (var connection = get();
                var stmt = connection.prepareStatement(sql);
                var rs = stmt.executeQuery()) {
            consumer.accept(rs);
        } catch (SQLException e) {
            throw new JdbcConnectionException(e);
        }
    }

    @Override
    public void assertQueriesNone(@NotNull String sql) {
        assertQueriesEquals(0, sql);
    }

    @Override
    public void assertQueriesAtLeast(int expectedAtLeast, @NotNull String sql) {
        assertQuery(sql, rs -> {
            int counter = 0;
            while (rs.next() && counter < expectedAtLeast) {
                counter++;
            }

            Assertions.assertEquals(expectedAtLeast, counter,
                    String.format("Expected to query at least %s rows but received %s for SQL: %s",
                            expectedAtLeast, counter, sql.replace("\n", " ")));
        });
    }

    @Override
    public void assertQueriesEquals(int expected, @NotNull String sql) {
        assertQuery(sql, rs -> {
            int counter = 0;
            while (rs.next()) {
                counter++;
            }

            Assertions.assertEquals(expected, counter,
                    String.format("Expected to query %s rows but received %s for SQL: %s",
                            expected, counter, sql.replace("\n", " ")));
        });
    }

    @Override
    public void assertInserted(@NotNull String sql) {
        logger.debug("Executing SQL:\n{}", sql);
        try (var connection = get();
                var stmt = connection.prepareStatement(sql)) {
            var rs = stmt.executeUpdate();
            if (rs == 0) {
                Assertions.fail(String.format("Expected query to update but it didn't for SQL: %s", sql.replace("\n", " ")));
            }
        } catch (SQLException e) {
            throw new JdbcConnectionException(e);
        }
    }

    @Override
    public void assertUpdated(@NotNull String sql) {
        assertInserted(sql);
    }

    @Override
    public void assertDeleted(@NotNull String sql) {
        assertInserted(sql);
    }

    @FunctionalInterface
    interface QueryChecker {

        boolean apply(@NotNull ResultSet rs) throws SQLException;
    }

    private boolean checkQuery(@Language("SQL") String sql, QueryChecker checker) {
        logger.debug("Executing SQL:\n{}", sql);
        try (var connection = get();
                var stmt = connection.prepareStatement(sql);
                var rs = stmt.executeQuery()) {
            return checker.apply(rs);
        } catch (Exception e) {
            logger.warn("Failed executing SQL:\n{}\nDue to: {}", sql, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean checkQueriesNone(@NotNull String sql) {
        return checkQueriesEquals(0, sql);
    }

    @Override
    public boolean checkQueriesAtLeast(int expectedAtLeast, @NotNull String sql) {
        return checkQuery(sql, rs -> {
            int counter = 0;
            while (rs.next() && counter < expectedAtLeast) {
                counter++;
            }

            return expectedAtLeast == counter;
        });
    }

    @Override
    public boolean checkQueriesEquals(int expected, @NotNull String sql) {
        return checkQuery(sql, rs -> {
            int counter = 0;
            while (rs.next()) {
                counter++;
            }

            return expected == counter;
        });
    }

    @Override
    public boolean checkInserted(@NotNull String sql) {
        logger.debug("Executing SQL: {}", sql);
        try (var connection = get();
                var stmt = connection.prepareStatement(sql)) {
            var rs = stmt.executeUpdate();
            return rs != 0;
        } catch (SQLException e) {
            throw new JdbcConnectionException(e);
        }
    }

    @Override
    public boolean checkUpdated(String sql) {
        return checkInserted(sql);
    }

    @Override
    public boolean checkDeleted(String sql) {
        return checkInserted(sql);
    }

    void stop() {
        this.isClosed = true;

        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                // do nothing
            }
            connection = null;
        }

        if (flywayJdbcMigrationEngine != null) {
            flywayJdbcMigrationEngine.close();
            flywayJdbcMigrationEngine = null;
        }
        if (liquibaseJdbcMigrationEngine != null) {
            liquibaseJdbcMigrationEngine.close();
            liquibaseJdbcMigrationEngine = null;
        }
    }

    @Override
    public void close() throws Exception {
        // do nothing
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        JdbcConnectionImpl that = (JdbcConnectionImpl) o;
        return Objects.equals(params, that.params) && Objects.equals(network, that.network);
    }

    @Override
    public int hashCode() {
        return Objects.hash(params, network);
    }

    @Override
    public String toString() {
        return params().jdbcUrl();
    }
}
