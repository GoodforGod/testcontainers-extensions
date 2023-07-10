package io.goodforgod.testcontainers.extensions.sql;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;

public interface SqlConnection {

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

    <T, E extends Throwable> Optional<T> executeForOne(@Language("SQL") String sql, @NotNull ResultSetMapper<T, E> extractor)
            throws E;

    default <T, E extends Throwable> T executeForOneOrNull(@Language("SQL") String sql, @NotNull ResultSetMapper<T, E> extractor)
            throws E {
        return executeForOne(sql, extractor).orElse(null);
    }
}
