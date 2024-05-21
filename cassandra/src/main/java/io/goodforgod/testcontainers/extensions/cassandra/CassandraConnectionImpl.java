package io.goodforgod.testcontainers.extensions.cassandra;

import com.datastax.oss.driver.api.core.CqlIdentifier;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.CqlSessionBuilder;
import com.datastax.oss.driver.api.core.config.DriverConfigLoader;
import com.datastax.oss.driver.api.core.config.OptionsMap;
import com.datastax.oss.driver.api.core.config.TypedDriverOption;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.internal.core.type.codec.registry.DefaultCodecRegistry;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Internal
class CassandraConnectionImpl implements CassandraConnection {

    private static final Duration TIMEOUT = Duration.ofSeconds(45);

    static final class ParamsImpl implements Params {

        private final String host;
        private final int port;
        private final String keyspace;
        private final String datacenter;
        private final String username;
        private final String password;

        ParamsImpl(String host, int port, String datacenter, String keyspace, String username, String password) {
            this.host = host;
            this.port = port;
            this.keyspace = keyspace;
            this.datacenter = datacenter;
            this.username = username;
            this.password = password;
        }

        @Override
        public String contactPoint() {
            return host + ":" + port;
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
        public @NotNull String keyspace() {
            return keyspace;
        }

        @Override
        public @NotNull String datacenter() {
            return datacenter;
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
            return "[host=" + host +
                    ", port=" + port +
                    ", datacenter=" + datacenter +
                    ", keyspace=" + keyspace +
                    ", username=" + username +
                    ", password=" + password + ']';
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(CassandraConnection.class);

    private final Params params;
    private final Params network;

    private volatile boolean isClosed = false;
    private volatile CqlSession connection;

    private volatile ScriptCassandraMigrationEngine scriptCassandraMigrationEngine;
    private volatile CognitorCassandraMigrationEngine cognitorCassandraMigrationEngine;

    CassandraConnectionImpl(Params params, Params network) {
        this.params = params;
        this.network = network;
    }

    static CassandraConnection forContainer(String host,
                                            int port,
                                            String hostInNetwork,
                                            Integer portInNetwork,
                                            String datacenter,
                                            String keyspace,
                                            String username,
                                            String password) {
        var params = new ParamsImpl(host, port, datacenter, keyspace, username, password);
        final Params network;
        if (hostInNetwork == null) {
            network = null;
        } else {
            network = new ParamsImpl(hostInNetwork, portInNetwork, datacenter, keyspace, username, password);
        }

        return new CassandraConnectionImpl(params, network);
    }

    static CassandraConnection forExternal(String host,
                                           int port,
                                           String datacenter,
                                           String keyspace,
                                           String username,
                                           String password) {
        var params = new CassandraConnectionImpl.ParamsImpl(host, port, keyspace, datacenter, username, password);
        return new CassandraConnectionImpl(params, null);
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
    public CqlSession getConnection() {
        if (isClosed) {
            throw new IllegalStateException("JdbcConnection was closed");
        }

        if (connection == null) {
            connection = openConnection();
        } else if (connection.isClosed()) {
            connection = openConnection();
        }

        return connection;
    }

    @NotNull
    private CqlSession openConnection() {
        logger.debug("Opening CQL connection...");

        OptionsMap optionsMap = OptionsMap.driverDefaults();
        optionsMap.put(TypedDriverOption.CONNECTION_CONNECT_TIMEOUT, TIMEOUT);
        optionsMap.put(TypedDriverOption.CONNECTION_INIT_QUERY_TIMEOUT, TIMEOUT);
        optionsMap.put(TypedDriverOption.CONNECTION_SET_KEYSPACE_TIMEOUT, TIMEOUT);
        var configLoader = DriverConfigLoader.fromMap(optionsMap);

        var sessionBuilder = new CqlSessionBuilder()
                .withCodecRegistry(new DefaultCodecRegistry("testing-codec-registry"))
                .withConfigLoader(configLoader)
                .withLocalDatacenter(params().datacenter())
                .addContactPoint(new InetSocketAddress(params().host(), params().port()));

        if (params().username() != null && params().password() != null) {
            sessionBuilder.withAuthCredentials(params().username(), params().password());
        }

        CqlSession session = sessionBuilder.build();
        createKeyspace(params().keyspace(), session);
        return session;
    }

    @Override
    public @NotNull CassandraMigrationEngine migrationEngine(Migration.@NotNull Engines engine) {
        if (engine == Migration.Engines.SCRIPTS) {
            if (scriptCassandraMigrationEngine == null) {
                this.scriptCassandraMigrationEngine = new ScriptCassandraMigrationEngine(this);
            }
            return this.scriptCassandraMigrationEngine;
        } else if (engine == Migration.Engines.COGNITOR) {
            if (cognitorCassandraMigrationEngine == null) {
                this.cognitorCassandraMigrationEngine = new CognitorCassandraMigrationEngine(this);
            }
            return this.cognitorCassandraMigrationEngine;
        }

        throw new UnsupportedOperationException("Unsupported engine: " + engine);
    }

    public void createKeyspace(@NotNull String keyspaceName) {
        createKeyspace(keyspaceName, getConnection());
    }

    private void createKeyspace(@NotNull String keyspaceName, CqlSession session) {
        try {
            String cql = "CREATE KEYSPACE IF NOT EXISTS " + keyspaceName
                    + " WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1};";
            var boundStatement = session.prepare(cql).bind().setTimeout(TIMEOUT);
            session.execute(boundStatement).wasApplied();
            session.execute("USE " + CqlIdentifier.fromCql(keyspaceName));
        } catch (Exception e) {
            throw new CassandraConnectionException(e);
        }
    }

    @Override
    public void execute(@Language("CQL") @NotNull String cql) {
        logger.debug("Executing CQL:\n{}", cql);
        try {
            var boundStatement = getConnection().prepare(cql).bind().setTimeout(TIMEOUT);
            getConnection().execute(boundStatement).wasApplied();
        } catch (Exception e) {
            throw new CassandraConnectionException(e);
        }
    }

    @Override
    public void executeFromResources(@NotNull String pathToResource) {
        logger.debug("Loading file from resources with path: {}", pathToResource);
        var resourceAsString = loadStringFromResources(pathToResource)
                .orElseThrow(() -> new IllegalArgumentException("Couldn't find resource with path: " + pathToResource));

        final List<String> queries = Arrays.stream(resourceAsString.split(";"))
                .map(query -> query + ";")
                .collect(Collectors.toList());
        for (String query : queries) {
            execute(query);
        }
    }

    private Optional<String> loadStringFromResources(String path) {
        try {
            try (var resource = CassandraConnectionImpl.class.getClassLoader().getResourceAsStream(path)) {
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
    public long count(@NotNull String table) {
        return queryOne("SELECT COUNT(*) FROM " + table, rs -> rs.getLong(0)).orElse(0L);
    }

    @Override
    public void assertCountsNone(@NotNull String table) {
        assertCountsEquals(0, table);
    }

    @Override
    public void assertCountsAtLeast(long expectedAtLeast, @NotNull String table) {
        final long count = count(table);
        if (count < expectedAtLeast) {
            Assertions.assertEquals(expectedAtLeast, count,
                    String.format("Expected to count in '%s' table at least %s rows but received %s",
                            table, expectedAtLeast, count));
        }
    }

    @Override
    public void assertCountsEquals(long expected, @NotNull String table) {
        final long count = count(table);
        Assertions.assertEquals(expected, count, String.format("Expected to count in '%s' table %s rows but received %s",
                table, expected, count));
    }

    @Override
    public <T, E extends Throwable> Optional<T> queryOne(@Language("CQL") @NotNull String cql,
                                                         @NotNull CassandraConnection.RowMapper<T, E> extractor)
            throws E {
        logger.debug("Executing CQL:\n{}", cql);
        try {
            var boundStatement = getConnection().prepare(cql).bind().setTimeout(Duration.ofMinutes(5));
            var row = getConnection().execute(boundStatement).one();
            return (row != null)
                    ? Optional.ofNullable(extractor.apply(row))
                    : Optional.empty();
        } catch (Exception e) {
            throw new CassandraConnectionException(e);
        }
    }

    @Override
    public <T, E extends Throwable> List<T> queryMany(@Language("CQL") @NotNull String cql,
                                                      @NotNull CassandraConnection.RowMapper<T, E> extractor)
            throws E {
        logger.debug("Executing CQL:\n{}", cql);
        try {
            var boundStatement = getConnection().prepare(cql).bind().setTimeout(TIMEOUT);
            var rows = getConnection().execute(boundStatement).all();
            final List<T> result = new ArrayList<>(rows.size());
            for (Row row : rows) {
                result.add(extractor.apply(row));
            }
            return result;
        } catch (Exception e) {
            throw new CassandraConnectionException(e);
        }
    }

    @FunctionalInterface
    interface QueryAssert {

        void accept(@NotNull ResultSet resultSet);
    }

    private void assertQuery(@Language("CQL") String cql, QueryAssert consumer) {
        logger.debug("Executing CQL:\n{}", cql);
        try {
            var boundStatement = getConnection().prepare(cql).bind().setTimeout(TIMEOUT);
            var rows = getConnection().execute(boundStatement);
            consumer.accept(rows);
        } catch (Exception e) {
            throw new CassandraConnectionException(e);
        }
    }

    @Override
    public void assertQueriesNone(@NotNull String cql) {
        assertQueriesEquals(0, cql);
    }

    @Override
    public void assertQueriesAtLeast(int expectedAtLeast, @NotNull String cql) {
        assertQuery(cql, rs -> {
            final int counter = Math.min(expectedAtLeast, rs.all().size());
            Assertions.assertEquals(expectedAtLeast, counter,
                    String.format("Expected to query at least %s rows but received %s for CQL: %s",
                            expectedAtLeast, counter, cql.replace("\n", " ")));
        });
    }

    @Override
    public void assertQueriesEquals(int expected, @NotNull String cql) {
        assertQuery(cql, rs -> {
            final int counter = rs.all().size();
            Assertions.assertEquals(expected, counter,
                    String.format("Expected to query %s rows but received %s for CQL: %s",
                            expected, counter, cql.replace("\n", " ")));
        });
    }

    @FunctionalInterface
    interface QueryChecker {

        boolean apply(@NotNull ResultSet resultSet);
    }

    private boolean checkQuery(@Language("CQL") String cql, QueryChecker checker) {
        logger.debug("Executing CQL:\n{}", cql);
        try {
            var boundStatement = getConnection().prepare(cql).bind().setTimeout(TIMEOUT);
            var rows = getConnection().execute(boundStatement);
            return checker.apply(rows);
        } catch (Exception e) {
            throw new CassandraConnectionException(e);
        }
    }

    @Override
    public boolean checkQueriesNone(@NotNull String cql) {
        return checkQueriesEquals(0, cql);
    }

    @Override
    public boolean checkQueriesAtLeast(int expectedAtLeast, @NotNull String cql) {
        return checkQuery(cql, rs -> {
            final int counter = Math.min(expectedAtLeast, rs.all().size());
            return expectedAtLeast <= counter;
        });
    }

    @Override
    public boolean checkQueriesEquals(int expected, @NotNull String cql) {
        return checkQuery(cql, rs -> {
            final int counter = rs.all().size();
            return expected == counter;
        });
    }

    void stop() {
        this.isClosed = true;
        if (connection != null) {
            connection.close();
            connection = null;
        }
    }

    @Override
    public void close() {
        // do nothing
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        CassandraConnectionImpl that = (CassandraConnectionImpl) o;
        return Objects.equals(params, that.params) && Objects.equals(network, that.network);
    }

    @Override
    public int hashCode() {
        return Objects.hash(params, network);
    }

    @Override
    public String toString() {
        return params().toString();
    }
}
