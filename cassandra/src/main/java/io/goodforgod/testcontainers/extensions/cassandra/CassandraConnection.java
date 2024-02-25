package io.goodforgod.testcontainers.extensions.cassandra;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import java.util.List;
import java.util.Optional;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.testcontainers.containers.CassandraContainer;

/**
 * Describes active Cassandra connection of currently running {@link CassandraContainer}
 */
public interface CassandraConnection extends AutoCloseable {

    @FunctionalInterface
    interface RowMapper<R, E extends Throwable> {

        /**
         * @param row to map
         * @return mapped value from row
         * @throws CassandraConnectionException if cql data manipulation error occurred
         * @throws E                            if mapping error occurred
         */
        R apply(@NotNull Row row) throws E;
    }

    /**
     * Cassandra connection parameters
     */
    interface Params {

        String contactPoint();

        @NotNull
        String host();

        int port();

        @NotNull
        String datacenter();

        String username();

        String password();
    }

    /**
     * @return connection parameters to container
     */
    @NotNull
    Params params();

    /**
     * @return connection parameters inside docker network, can be useful when one container require
     *             params to connect to Cassandra Database container inside docker network
     */
    @NotNull
    Optional<Params> paramsInNetwork();

    /**
     * NOTE: DO NOT CLOSE CONNECTION
     *
     * @return new Cassandra connection
     */
    @NotNull
    CqlSession get();

    /**
     * @param keyspaceName to create
     */
    void createKeyspace(@NotNull String keyspaceName);

    /**
     * @param cql to execute
     */
    void execute(@NotNull @Language("CQL") String cql);

    /**
     * @param pathToResource path to CQL code to execute
     */
    void executeFromResources(@NotNull String pathToResource);

    /**
     * @param cql       to execute and retrieve results from
     * @param extractor map executed CQL query applied to each {@link ResultSet} after
     *                  {@link ResultSet#all()} is invoked
     * @param <T>       mapped type
     * @param <E>       error type
     * @return mapped entity if {@link ResultSet#all()} was true or {@link Optional#empty()} otherwise
     * @throws E of error
     */
    <T, E extends Throwable> Optional<T> queryOne(@NotNull @Language("CQL") String cql,
                                                  @NotNull CassandraConnection.RowMapper<T, E> extractor)
            throws E;

    /**
     * @param cql       to execute and retrieve results from
     * @param extractor map executed CQL query applied to each {@link ResultSet} after
     *                  {@link ResultSet#all()} is invoked
     * @param <T>       mapped type
     * @param <E>       error type
     * @return mapped list of entities if {@link ResultSet#all()} found any or {@link Optional#empty()}
     *             otherwise
     * @throws E of error
     */
    <T, E extends Throwable> List<T> queryMany(@NotNull @Language("CQL") String cql,
                                               @NotNull CassandraConnection.RowMapper<T, E> extractor)
            throws E;

    /**
     * @param table example: mykeyspace.mytable
     * @return SELECT COUNT(*) from specified table
     */
    long count(@NotNull String table);

    /**
     * Asserts that SELECT COUNT(*) in specified table counts 0 rows
     *
     * @param table example: mykeyspace.mytable
     */
    void assertCountsNone(@NotNull String table);

    /**
     * Asserts that SELECT COUNT(*) in specified table counts at least minimal number expectedAtLeast
     * rows
     *
     * @param table           example: mykeyspace.mytable
     * @param expectedAtLeast at least minimal number of rows expected
     */
    void assertCountsAtLeast(long expectedAtLeast, @NotNull String table);

    /**
     * Asserts that SELECT COUNT(*) in specified table counts exact number expected rows
     *
     * @param table    example: mykeyspace.mytable
     * @param expected exact number of rows expected
     */
    void assertCountsEquals(long expected, @NotNull String table);

    /**
     * Asserts that executed CQL results in 0 rows
     *
     * @param cql to execute
     */
    void assertQueriesNone(@NotNull @Language("CQL") String cql);

    /**
     * Asserts that executed CQL results in at least minimal number of expectedAtLeast rows
     *
     * @param cql             to execute
     * @param expectedAtLeast at least minimal number of rows expected
     */
    void assertQueriesAtLeast(int expectedAtLeast, @NotNull @Language("CQL") String cql);

    /**
     * Asserts that executed CQL results in exact number of expected rows
     *
     * @param cql      to execute
     * @param expected exact number of rows expected
     */
    void assertQueriesEquals(int expected, @NotNull @Language("CQL") String cql);

    /**
     * @param cql to execute
     * @return true if executed CQL results in 0 rows
     */
    boolean checkQueriesNone(@NotNull @Language("CQL") String cql);

    /**
     * @param cql             to execute
     * @param expectedAtLeast at least minimal number of rows expected
     * @return true if executed CQL results in at least minimal number expectedAtLeast rows
     */
    boolean checkQueriesAtLeast(int expectedAtLeast, @NotNull @Language("CQL") String cql);

    /**
     * @param cql      to execute
     * @param expected exact number of rows expected
     * @return true if executed CQL results in exact number of expected rows
     */
    boolean checkQueriesEquals(int expected, @NotNull @Language("CQL") String cql);

    static CassandraConnection forContainer(CassandraContainer<?> container) {
        var params = new CassandraConnectionImpl.ParamsImpl(container.getHost(),
                container.getMappedPort(CassandraContainer.CQL_PORT),
                container.getLocalDatacenter(), container.getUsername(), container.getPassword());
        final Params network = new CassandraConnectionImpl.ParamsImpl(container.getNetworkAliases().get(0),
                CassandraContainer.CQL_PORT,
                container.getLocalDatacenter(), container.getUsername(), container.getPassword());
        return new CassandraConnectionClosableImpl(params, network);
    }

    static CassandraConnection forParams(String host,
                                         int port,
                                         String datacenter,
                                         String username,
                                         String password) {
        var params = new CassandraConnectionImpl.ParamsImpl(host, port, datacenter, username, password);
        return new CassandraConnectionClosableImpl(params, null);
    }
}
