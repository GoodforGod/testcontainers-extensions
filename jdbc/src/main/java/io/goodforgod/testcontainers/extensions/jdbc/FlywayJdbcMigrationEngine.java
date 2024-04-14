package io.goodforgod.testcontainers.extensions.jdbc;

import java.nio.charset.StandardCharsets;
import java.util.List;
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

    private static Flyway getFlyway(DataSource dataSource, List<String> locations) {
        final List<String> migrationLocations = (locations.isEmpty())
                ? List.of("classpath:db/migration")
                : locations;

        return Flyway.configure()
                .dataSource(dataSource)
                .loggers("slf4j")
                .connectRetries(5)
                .connectRetriesInterval(1)
                .encoding(StandardCharsets.UTF_8)
                .locations(migrationLocations.toArray(String[]::new))
                .cleanDisabled(false)
                .load();
    }

    @Override
    public void apply(@NotNull List<String> locations) {
        logger.debug("Starting migration migration for engine '{}' for connection: {}",
                getClass().getSimpleName(), jdbcConnection);

        try {
            getFlyway(getDataSource(), locations).migrate();
        } catch (Exception e) {
            try {
                Thread.sleep(250);
                getFlyway(getDataSource(), locations).migrate();
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
