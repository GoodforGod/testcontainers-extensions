package io.goodforgod.testcontainers.extensions.jdbc;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FlywayJdbcMigrationEngine implements JdbcMigrationEngine, AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(FlywayJdbcMigrationEngine.class);

    private final JdbcConnection jdbcConnection;

    private volatile HikariDataSource dataSource;

    public FlywayJdbcMigrationEngine(JdbcConnection jdbcConnection) {
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
        logger.debug("Starting schema migration for engine '{}' for connection: {}",
                getClass().getSimpleName(), jdbcConnection);

        try {
            getFlyway(getDataSource(), locations).migrate();
        } catch (Exception e) {
            try {
                Thread.sleep(250);
                getFlyway(getDataSource(), locations).migrate();
            } catch (InterruptedException ex) {
                logger.error("Failed schema migration for engine '{}' for connection: {}",
                        getClass().getSimpleName(), jdbcConnection);

                throw new IllegalStateException(ex);
            }
        }

        logger.info("Finished schema migration for engine '{}' for connection: {}",
                getClass().getSimpleName(), jdbcConnection);
    }

    @Override
    public void drop(@NotNull List<String> locations) {
        logger.debug("Starting schema dropping for engine '{}' for connection: {}",
                getClass().getSimpleName(), jdbcConnection);

        getFlyway(getDataSource(), locations).clean();

        logger.info("Finished schema dropping for engine '{}' for connection: {}",
                getClass().getSimpleName(), jdbcConnection);
    }

    private HikariDataSource getDataSource() {
        if (this.dataSource == null) {
            HikariConfig hikariConfig = new HikariConfig();
            hikariConfig.setJdbcUrl(jdbcConnection.params().jdbcUrl());
            hikariConfig.setUsername(jdbcConnection.params().username());
            hikariConfig.setPassword(jdbcConnection.params().password());
            hikariConfig.setAutoCommit(true);
            hikariConfig.setMinimumIdle(1);
            hikariConfig.setMaximumPoolSize(10);
            hikariConfig.setPoolName("flyway");
            this.dataSource = new HikariDataSource(hikariConfig);
        }
        return this.dataSource;
    }

    @Override
    public void close() {
        if (dataSource != null) {
            dataSource.close();
            dataSource = null;
        }
    }
}
