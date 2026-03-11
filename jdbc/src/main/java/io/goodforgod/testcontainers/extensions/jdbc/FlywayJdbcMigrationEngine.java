package io.goodforgod.testcontainers.extensions.jdbc;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FlywayJdbcMigrationEngine implements JdbcMigrationEngine, AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(FlywayJdbcMigrationEngine.class);

    private final JdbcConnectionImpl jdbcConnection;

    public FlywayJdbcMigrationEngine(JdbcConnectionImpl jdbcConnection) {
        this.jdbcConnection = jdbcConnection;
    }

    private Flyway getFlyway(DataSource dataSource, List<String> locations) {
        final List<String> migrationLocations = (locations.isEmpty())
                ? List.of("classpath:db/migration")
                : locations;

        Map<String, String> configMap = Map.of();
        if (this.jdbcConnection.params().jdbcUrl().startsWith("jdbc:postgresql://")) {
            configMap = Map.of("flyway.postgresql.transactional.lock", "false");
        }

        return Flyway.configure()
                .dataSource(dataSource)
                .loggers("slf4j")
                .connectRetries(5)
                .connectRetriesInterval(1)
                .cleanDisabled(false)
                .executeInTransaction(false)
                .configuration(configMap)
                .encoding(StandardCharsets.UTF_8)
                .locations(migrationLocations.toArray(String[]::new))
                .load();
    }

    @Override
    public void apply(@NotNull List<String> locations) {
        logger.debug("Starting migration migration for engine '{}' for connection: {}",
                getClass().getSimpleName(), jdbcConnection);

        Flyway flyway = getFlyway(getDataSource(), locations);
        try {
            flyway.migrate();
        } catch (Exception e) {
            try {
                Thread.sleep(250);
                flyway.migrate();
            } catch (InterruptedException ex) {
                logger.error("Failed migration migration for engine '{}' for connection: {}",
                        getClass().getSimpleName(), jdbcConnection);

                throw new IllegalStateException(ex);
            }
        }

        logger.info("Finished migration migration for engine '{}' for connection: {}",
                getClass().getSimpleName(), jdbcConnection);
    }

    @Override
    public void drop(@NotNull List<String> locations) {
        logger.debug("Starting migration dropping for engine '{}' for connection: {}",
                getClass().getSimpleName(), jdbcConnection);

        getFlyway(getDataSource(), locations).clean();

        logger.info("Finished migration dropping for engine '{}' for connection: {}",
                getClass().getSimpleName(), jdbcConnection);
    }

    private DataSource getDataSource() {
        return this.jdbcConnection.dataSource();
    }

    @Override
    public void close() {
        // do nothing
    }
}
