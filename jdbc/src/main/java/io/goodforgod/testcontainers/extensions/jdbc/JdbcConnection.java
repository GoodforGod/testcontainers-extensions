package io.goodforgod.testcontainers.extensions.jdbc;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;

public interface JdbcConnection {

    @FunctionalInterface
    interface ResultSetMapper<R, E extends Throwable> {

        R apply(@NotNull ResultSet currentRow) throws SQLException, E;
    }

    @NotNull
    String host();

    int port();

    @NotNull
    String database();

    String username();

    String password();

    @NotNull
    String jdbcUrl();

    /**
     * @return new JDBC connection
     */
    @NotNull
    Connection open();

    /**
     * @param sql to execute
     */
    void execute(@NotNull @Language("SQL") String sql);

    /**
     * @param pathToFileInResources path to SQL code to execute
     */
    void executeFromResources(@NotNull String pathToFileInResources);

    /**
     * @param sql       to execute and retrieve results from
     * @param extractor map executed SQL query applied to each {@link ResultSet} after
     *                  {@link ResultSet#next()} is invoked
     * @return mapped entity if {@link ResultSet#next()} was true or {@link Optional#empty()} otherwise
     * @param <T> mapped type
     * @param <E> error type
     * @throws E of error
     */
    <T, E extends Throwable> Optional<T> queryOne(@NotNull @Language("SQL") String sql,
                                                  @NotNull ResultSetMapper<T, E> extractor)
            throws E;

    /**
     * @param sql       to execute and retrieve results from
     * @param extractor map executed SQL query applied to each {@link ResultSet} after
     *                  {@link ResultSet#next()} is invoked
     * @return mapped list of entities if {@link ResultSet#next()} found any or {@link Optional#empty()}
     *             otherwise
     * @param <T> mapped type
     * @param <E> error type
     * @throws E of error
     */
    <T, E extends Throwable> List<T> queryMany(@NotNull @Language("SQL") String sql,
                                               @NotNull ResultSetMapper<T, E> extractor)
            throws E;

    /**
     * @param tableName to SELECT COUNT(*) in
     * @return SELECT COUNT(*) from specified table
     */
    int count(@NotNull String tableName);

    /**
     * Asserts that SELECT COUNT(*) in specified table counts 0 rows
     * 
     * @param tableName to SELECT COUNT(*) in
     */
    void assertCountsNone(@NotNull String tableName);

    /**
     * Asserts that SELECT COUNT(*) in specified table counts at least minimal number expectedInTable
     * rows
     * 
     * @param tableName       to SELECT COUNT(*) in
     * @param expectedInTable at least minimal number of rows expected
     */
    void assertCountsAtLeast(int expectedInTable, @NotNull String tableName);

    /**
     * Asserts that SELECT COUNT(*) in specified table counts exact number expectedInTable rows
     * 
     * @param tableName       to SELECT COUNT(*) in
     * @param expectedInTable exact number of rows expected
     */
    void assertCountsExact(int expectedInTable, @NotNull String tableName);

    /**
     * Asserts that executed SQL results in 0 rows
     * 
     * @param sql to execute
     */
    void assertQueriesNone(@NotNull @Language("SQL") String sql);

    /**
     * Asserts that executed SQL results in at least minimal number expectedInTable rows
     * 
     * @param sql          to execute
     * @param expectedRows at least minimal number of rows expected
     */
    void assertQueriesAtLeast(int expectedRows, @NotNull @Language("SQL") String sql);

    /**
     * Asserts that executed SQL results in exact number of rows expected
     * 
     * @param sql          to execute
     * @param expectedRows exact number of rows expected
     */
    void assertQueriesExact(int expectedRows, @NotNull @Language("SQL") String sql);

    /**
     * Asserts that executed SQL results in any inserted entities
     * 
     * @param sql to execute
     */
    void assertInserted(@NotNull @Language("SQL") String sql);

    /**
     * Asserts that executed SQL results in any updated entities
     * 
     * @param sql to execute
     */
    void assertUpdated(@NotNull @Language("SQL") String sql);

    /**
     * Asserts that executed SQL results in any deleted entities
     * 
     * @param sql to execute
     */
    void assertDeleted(@NotNull @Language("SQL") String sql);

    /**
     * @param sql to execute
     * @return true if executed SQL results in 0 rows
     */
    boolean checkQueriesNone(@NotNull @Language("SQL") String sql);

    /**
     * @param sql          to execute
     * @param expectedRows at least minimal number of rows expected
     * @return true if executed SQL results in at least minimal number expectedInTable rows
     */
    boolean checkQueriesAtLeast(int expectedRows, @NotNull @Language("SQL") String sql);

    /**
     * @param sql          to execute
     * @param expectedRows exact number of rows expected
     * @return true if executed SQL results in exact number of rows expected
     */
    boolean checkQueriesExact(int expectedRows, @NotNull @Language("SQL") String sql);

    /**
     * @param sql to execute
     * @return true if executed SQL results in any inserted entities
     */
    boolean checkInserted(@NotNull @Language("SQL") String sql);

    /**
     * @param sql to execute
     * @return true if executed SQL results in any updated entities
     */
    boolean checkUpdated(@Language("SQL") String sql);

    /**
     * @param sql to execute
     * @return true if executed SQL results in any deleted entities
     */
    boolean checkDeleted(@Language("SQL") String sql);
}
