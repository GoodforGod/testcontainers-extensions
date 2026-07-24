package io.goodforgod.testcontainers.extensions.postgres;

import static org.junit.jupiter.api.Assertions.*;

import io.goodforgod.testcontainers.extensions.ContainerMode;
import io.goodforgod.testcontainers.extensions.Isolation;
import io.goodforgod.testcontainers.extensions.jdbc.ConnectionPostgreSQL;
import io.goodforgod.testcontainers.extensions.jdbc.JdbcConnection;
import io.goodforgod.testcontainers.extensions.jdbc.Migration;
import io.goodforgod.testcontainers.extensions.jdbc.TestcontainersPostgreSQL;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Execution(ExecutionMode.CONCURRENT)
@TestcontainersPostgreSQL(mode = ContainerMode.PER_RUN,
        image = "postgres:17.6-alpine",
        isolation = @Isolation(Isolation.Mode.PER_METHOD),
        migration = @Migration(
                engine = Migration.Engines.FLYWAY,
                apply = Migration.Mode.PER_CLASS,
                drop = Migration.Mode.NONE,
                strategy = Migration.Strategy.TEMPLATE_CLONE))
class PostgresIsolationPerMethodTests {

    private static final Set<String> DATABASES = ConcurrentHashMap.newKeySet();

    @ConnectionPostgreSQL
    private JdbcConnection connection;

    @Test
    void firstRun(@ConnectionPostgreSQL JdbcConnection parameter) {
        assertIsolated(parameter);
    }

    @Test
    void secondRun(@ConnectionPostgreSQL JdbcConnection parameter) {
        assertIsolated(parameter);
    }

    private void assertIsolated(JdbcConnection parameter) {
        assertSame(connection, parameter);
        assertTrue(connection.params().database().startsWith("postgres_"));
        assertTrue(DATABASES.add(connection.params().database()));

        connection.execute("INSERT INTO users VALUES(1);");
        connection.assertCountsEquals(1, "users");
    }
}
