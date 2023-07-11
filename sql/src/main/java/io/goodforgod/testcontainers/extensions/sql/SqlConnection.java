package io.goodforgod.testcontainers.extensions.sql;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;

public interface SqlConnection {

    @NotNull
    static SqlConnection create(@NotNull String host, int port, @NotNull String database) {
        return create(host, port, database, null, null);
    }

    @NotNull
    static SqlConnection create(@NotNull String host, int port, @NotNull String database, String username, String password) {
        return new SqlConnectionImpl(host, port, database, username, password);
    }

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

    @NotNull
    String r2dbcUrl();

    @NotNull
    Connection open();

    void execute(@Language("SQL") String sql);

    <T, E extends Throwable> Optional<T> queryOne(@Language("SQL") String sql, @NotNull ResultSetMapper<T, E> extractor) throws E;

    <T, E extends Throwable> List<T> queryMany(@Language("SQL") String sql, @NotNull ResultSetMapper<T, E> extractor) throws E;

    void assertQueriesNone(@Language("SQL") String sql);

    void assertQueriesAtLeastOne(@Language("SQL") String sql);

    void assertQueriesAtLeast(@Language("SQL") String sql, int expectedRows);

    void assertQueriesExactOne(@Language("SQL") String sql);

    void assertQueriesExact(@Language("SQL") String sql, int expectedRows);

    void assertInserted(@Language("SQL") String sql);

    void assertUpdated(@Language("SQL") String sql);

    void assertDeleted(@Language("SQL") String sql);

    boolean checkQueriesNone(@Language("SQL") String sql);

    boolean checkQueriesAtLeastOne(@Language("SQL") String sql);

    boolean checkQueriesAtLeast(@Language("SQL") String sql, int expectedRows);

    boolean checkQueriesExactOne(@Language("SQL") String sql);

    boolean checkQueriesExact(@Language("SQL") String sql, int expectedRows);

    boolean checkInserted(@Language("SQL") String sql);

    boolean checkUpdated(@Language("SQL") String sql);

    boolean checkDeleted(@Language("SQL") String sql);
}
