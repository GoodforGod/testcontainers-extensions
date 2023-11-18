package io.goodforgod.testcontainers.extensions.jdbc;

import java.nio.charset.StandardCharsets;
import java.util.List;
import org.flywaydb.core.Flyway;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FlywayJdbcMigrationEngine implements JdbcMigrationEngine {

    private static final Logger logger = LoggerFactory.getLogger(FlywayJdbcMigrationEngine.class);

    public static final JdbcMigrationEngine INSTANCE = new FlywayJdbcMigrationEngine();

    private FlywayJdbcMigrationEngine() {}

    private static Flyway getFlyway(JdbcConnection connection, List<String> locations) {
        final List<String> migrationLocations = (locations.isEmpty())
                ? List.of("classpath:db/migration")
                : locations;

        return Flyway.configure()
                .loggers("slf4j")
                .connectRetries(5)
                .connectRetriesInterval(1)
                .encoding(StandardCharsets.UTF_8)
                .dataSource(connection.params().jdbcUrl(), connection.params().username(), connection.params().password())
                .locations(migrationLocations.toArray(String[]::new))
                .cleanDisabled(false)
                .load();
    }

    @Override
    public void migrate(@NotNull JdbcConnection connection, @NotNull List<String> locations) {
        if (locations.isEmpty()) {
            logger.warn("Empty locations for schema migration for engine '{}' for connection: {}",
                    getClass().getSimpleName(), connection);
            return;
        }

        logger.debug("Starting schema migration for engine '{}' for connection: {}",
                getClass().getSimpleName(), connection);

        try {
            getFlyway(connection, locations).migrate();
        } catch (Exception e) {
            try {
                Thread.sleep(250);
                getFlyway(connection, locations).migrate();

                logger.debug("Finished schema migration for engine '{}' for connection: {}",
                        getClass().getSimpleName(), connection);
            } catch (InterruptedException ex) {
                logger.error("Failed schema migration for engine '{}' for connection: {}",
                        getClass().getSimpleName(), connection);

                throw new IllegalStateException(ex);
            }
        }
    }

    @Override
    public void drop(@NotNull JdbcConnection connection, @NotNull List<String> locations) {
        if (locations.isEmpty()) {
            logger.warn("Empty locations for schema migration for engine '{}' for connection: {}",
                    getClass().getSimpleName(), connection);
            return;
        }

        logger.debug("Starting schema dropping for engine '{}' for connection: {}",
                getClass().getSimpleName(), connection);

        getFlyway(connection, locations).clean();
    }
}
