package io.goodforgod.testcontainers.extensions.sql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

record SqlConnectionImpl(String host,
                         int port,
                         String database,
                         String username,
                         String password)
        implements SqlConnection {

    private static final Logger logger = LoggerFactory.getLogger(SqlConnection.class);

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
            logger.debug("Opening SQL connection...");
            return DriverManager.getConnection(jdbcUrl(), username, password);
        } catch (SQLException e) {
            throw new SqlConnectionException(e);
        }
    }

    @Override
    public void execute(@Language("SQL") String sql) {
        logger.debug("Executing SQL: {}", sql);
        try (var connection = open();
                var stmt = connection.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            throw new SqlConnectionException(e);
        }
    }

    @Override
    public <T, E extends Throwable> Optional<T> queryOne(@Language("SQL") String sql,
                                                         @NotNull ResultSetMapper<T, E> extractor)
            throws E {
        logger.debug("Executing SQL: {}", sql);
        try (var connection = open();
                var stmt = connection.prepareStatement(sql);
                var rs = stmt.executeQuery()) {
            return (rs.next())
                    ? Optional.ofNullable(extractor.apply(rs))
                    : Optional.empty();
        } catch (SQLException e) {
            throw new SqlConnectionException(e);
        }
    }

    @Override
    public <T, E extends Throwable> List<T> queryMany(@Language("SQL") String sql, @NotNull ResultSetMapper<T, E> extractor)
            throws E {
        logger.debug("Executing SQL: {}", sql);
        try (var connection = open();
                var stmt = connection.prepareStatement(sql);
                var rs = stmt.executeQuery()) {
            final List<T> result = new ArrayList<>();
            while (rs.next()) {
                result.add(extractor.apply(rs));
            }
            return result;
        } catch (SQLException e) {
            throw new SqlConnectionException(e);
        }
    }

    @FunctionalInterface
    interface QueryAssert {

        void accept(@NotNull ResultSet rs) throws SQLException;
    }

    private void assertQuery(String sql, QueryAssert consumer) {
        logger.debug("Executing SQL: {}", sql);
        try (var connection = open();
                var stmt = connection.prepareStatement(sql);
                var rs = stmt.executeQuery()) {
            consumer.accept(rs);
        } catch (SQLException e) {
            throw new SqlConnectionException(e);
        }
    }

    @Override
    public void assertQueriesNone(String sql) {
        assertQueriesExact(sql, 0);
    }

    @Override
    public void assertQueriesAtLeastOne(String sql) {
        assertQueriesAtLeast(sql, 1);
    }

    @Override
    public void assertQueriesAtLeast(String sql, int expectedRows) {
        assertQuery(sql, rs -> {
            int counter = 0;
            while (rs.next() && counter < expectedRows) {
                counter++;
            }

            Assertions.assertEquals(expectedRows, counter, "Expected to query %s rows but received %s for SQL: %s".formatted(
                    expectedRows, counter, sql.replace("\n", " ")));
        });
    }

    @Override
    public void assertQueriesExactOne(String sql) {
        assertQueriesExact(sql, 1);
    }

    @Override
    public void assertQueriesExact(String sql, int expectedRows) {
        assertQuery(sql, rs -> {
            int counter = 0;
            while (rs.next()) {
                counter++;
            }

            Assertions.assertEquals(expectedRows, counter, "Expected to query %s rows but received %s for SQL: %s".formatted(
                    expectedRows, counter, sql.replace("\n", " ")));
        });
    }

    @Override
    public void assertInserted(String sql) {
        logger.debug("Executing SQL: {}", sql);
        try (var connection = open();
                var stmt = connection.prepareStatement(sql)) {
            var rs = stmt.executeUpdate();
            if (rs == 0) {
                Assertions.fail("Expected query to update but it didn't for SQL: %s".formatted(sql.replace("\n", " ")));
            }
        } catch (SQLException e) {
            throw new SqlConnectionException(e);
        }
    }

    @Override
    public void assertUpdated(String sql) {
        assertInserted(sql);
    }

    @Override
    public void assertDeleted(String sql) {
        assertInserted(sql);
    }

    @FunctionalInterface
    interface QueryChecker {

        boolean apply(@NotNull ResultSet rs) throws SQLException;
    }

    private boolean checkQuery(String sql, QueryChecker checker) {
        logger.debug("Executing SQL: {}", sql);
        try (var connection = open();
                var stmt = connection.prepareStatement(sql);
                var rs = stmt.executeQuery()) {
            return checker.apply(rs);
        } catch (Exception e) {
            logger.warn("Failed executing SQL: {}\nDue to: {}", sql, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean checkQueriesNone(String sql) {
        return checkQueriesExact(sql, 0);
    }

    @Override
    public boolean checkQueriesAtLeastOne(String sql) {
        return checkQueriesAtLeast(sql, 1);
    }

    @Override
    public boolean checkQueriesAtLeast(String sql, int expectedRows) {
        return checkQuery(sql, rs -> {
            int counter = 0;
            while (rs.next() && counter < expectedRows) {
                counter++;
            }

            return expectedRows == counter;
        });
    }

    @Override
    public boolean checkQueriesExactOne(String sql) {
        return checkQueriesExact(sql, 1);
    }

    @Override
    public boolean checkQueriesExact(String sql, int expectedRows) {
        return checkQuery(sql, rs -> {
            int counter = 0;
            while (rs.next()) {
                counter++;
            }

            return expectedRows == counter;
        });
    }

    @Override
    public boolean checkInserted(String sql) {
        logger.debug("Executing SQL: {}", sql);
        try (var connection = open();
                var stmt = connection.prepareStatement(sql)) {
            var rs = stmt.executeUpdate();
            return rs != 0;
        } catch (SQLException e) {
            throw new SqlConnectionException(e);
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

    @Override
    public String toString() {
        return jdbcUrl();
    }
}
