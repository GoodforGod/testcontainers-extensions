package io.goodforgod.testcontainers.extensions.jdbc;

import java.io.FileWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.DatabaseFactory;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

public final class LiquibaseJdbcMigrationEngine implements JdbcMigrationEngine {

    private static final Logger logger = LoggerFactory.getLogger(LiquibaseJdbcMigrationEngine.class);

    public static final JdbcMigrationEngine INSTANCE = new LiquibaseJdbcMigrationEngine();

    private static volatile boolean isLiquibaseActivated = false;

    private LiquibaseJdbcMigrationEngine() {}

    @FunctionalInterface
    private interface LiquibaseRunner {

        void apply(Liquibase liquibase, Writer writer) throws LiquibaseException;
    }

    private static void prepareLiquibase(JdbcConnection connection, List<String> locations, LiquibaseRunner liquibaseConsumer) {
        try {
            final List<String> changeLogLocations = (locations.isEmpty())
                    ? List.of("db/changelog.sql")
                    : locations;

            if (!isLiquibaseActivated) {
                final boolean julEnabled = Optional.ofNullable(System.getenv("TEST_CONTAINERS_EXTENSION_JDBC_JUL_ENABLED"))
                        .map(Boolean::parseBoolean)
                        .orElse(true);

                if (julEnabled) {
                    SLF4JBridgeHandler.removeHandlersForRootLogger();
                    SLF4JBridgeHandler.install();
                    isLiquibaseActivated = true;
                }
            }

            try (var con = connection.open()) {
                var liquibaseConnection = new liquibase.database.jvm.JdbcConnection(con);
                var database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(liquibaseConnection);
                for (String changeLog : changeLogLocations) {
                    try (var classLoaderResourceAccessor = new ClassLoaderResourceAccessor()) {
                        try (var liquibase = new Liquibase(changeLog, classLoaderResourceAccessor, database)) {
                            var tmpFile = Files.createTempFile("liquibase-changelog-output", ".txt");
                            try (var writer = new FileWriter(tmpFile.toFile())) {
                                liquibaseConsumer.apply(liquibase, writer);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static void migrateLiquibase(JdbcConnection connection, List<String> locations) {
        prepareLiquibase(connection, locations, (liquibase, writer) -> {
            var contexts = new Contexts();
            var labelExpression = new LabelExpression();
            var changeSetStatuses = liquibase.getChangeSetStatuses(contexts, labelExpression, true);
            if (!changeSetStatuses.isEmpty()) {
                liquibase.update();
            }
        });
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
            migrateLiquibase(connection, locations);
        } catch (Exception e) {
            try {
                Thread.sleep(250);
                migrateLiquibase(connection, locations);

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
            logger.warn("Empty locations for schema drop for engine '{}' for connection: {}",
                    getClass().getSimpleName(), connection);
            return;
        }

        logger.debug("Starting schema dropping for engine '{}' for connection: {}",
                getClass().getSimpleName(), connection);

        prepareLiquibase(connection, locations, (liquibase, writer) -> liquibase.dropAll());
    }
}
