package io.goodforgod.testcontainers.extensions.sql;

import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.testcontainers.containers.JdbcDatabaseContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Optional;

record SqlConnectionImpl(String host, int port, String database, String username, String password) implements SqlConnection {

    SqlConnectionImpl(JdbcDatabaseContainer<?> container, int mappedPort) {
        this(container.getHost(),
            container.getMappedPort(mappedPort),
            container.getDatabaseName(),
            container.getUsername(),
            container.getPassword());
    }

    @NotNull
    @Override
    public String jdbcUrl() {
        return "jdbc:postgresql://%s:%d/%s".formatted(host, port, database);
    }

    @NotNull
    @Override
    public String r2dbcUrl() {
        return "r2dbc:postgresql://%s:%d/%s".formatted(host, port, database);
    }

    @NotNull
    @Override
    public Connection open() {
        try {
            return DriverManager.getConnection(jdbcUrl(), username, password);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void execute(@Language("SQL") String sql) {
        try (var connection = open();
             var stmt = connection.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public <T, E extends Throwable> Optional<T> executeForOne(@Language("SQL") String sql, @NotNull ResultSetMapper<T, E> extractor) throws E {
        try (var connection = open();
             var stmt = connection.prepareStatement(sql);
             var rs = stmt.executeQuery()) {
            return (rs.next())
                    ? Optional.ofNullable(extractor.apply(rs))
                    : Optional.empty();
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }
}
