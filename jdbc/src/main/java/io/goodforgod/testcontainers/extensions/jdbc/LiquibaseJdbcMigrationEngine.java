package io.goodforgod.testcontainers.extensions.jdbc;

import java.io.FileWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.sql.Connection;
import java.util.List;
import java.util.Optional;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.exception.DatabaseException;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

public final class LiquibaseJdbcMigrationEngine implements JdbcMigrationEngine, AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(LiquibaseJdbcMigrationEngine.class);

    private static volatile boolean isLiquibaseActivated = false;

    @FunctionalInterface
    private interface LiquibaseRunner {

        void apply(Liquibase liquibase, Writer writer) throws LiquibaseException;
    }

    private final JdbcConnection jdbcConnection;

    public LiquibaseJdbcMigrationEngine(JdbcConnection jdbcConnection) {
        this.jdbcConnection = jdbcConnection;
    }

    private static void prepareLiquibase(Database database, List<String> locations, LiquibaseRunner liquibaseConsumer) {
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

            try (var classLoaderResourceAccessor = new ClassLoaderResourceAccessor()) {
                for (String changeLog : changeLogLocations) {
                    var liquibase = new Liquibase(changeLog, classLoaderResourceAccessor, database);
                    var tmpFile = Files.createTempFile("liquibase-changelog-output", ".txt");
                    try (var writer = new FileWriter(tmpFile.toFile())) {
                        liquibaseConsumer.apply(liquibase, writer);
                    }
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static void migrateLiquibase(Database database, List<String> locations) {
        prepareLiquibase(database, locations, (liquibase, writer) -> {
            var contexts = new Contexts();
            var labelExpression = new LabelExpression();
            var changeSetStatuses = liquibase.getChangeSetStatuses(contexts, labelExpression, true);
            if (!changeSetStatuses.isEmpty()) {
                liquibase.update();
                database.commit();
            }
        });
    }

    private static void dropLiquibase(Database database, List<String> locations) {
        prepareLiquibase(database, locations, (liquibase, writer) -> {
            liquibase.dropAll();
            database.commit();
        });
    }

    @Override
    public void apply(@NotNull List<String> locations) {
        logger.debug("Starting migration apply for engine '{}' for connection: {}",
                getClass().getSimpleName(), jdbcConnection);

        try {
            try (Connection connection = jdbcConnection.openConnection()) {
                try (var database = getLiquiDatabase(connection)) {
                    migrateLiquibase(database, locations);
                }
            }
        } catch (Exception e) {
            try {
                Thread.sleep(250);
                try (Connection connection = jdbcConnection.openConnection()) {
                    try (var database = getLiquiDatabase(connection)) {
                        migrateLiquibase(database, locations);
                    }
                }
            } catch (Exception ex) {
                logger.error("Failed migration apply for engine '{}' for connection: {}",
                        getClass().getSimpleName(), jdbcConnection);

                throw new IllegalStateException(ex);
            }
        }

        logger.info("Finished migration apply for engine '{}' for connection: {}",
                getClass().getSimpleName(), jdbcConnection);
    }

    @Override
    public void drop(@NotNull List<String> locations) {
        logger.debug("Starting migration dropping for engine '{}' for connection: {}",
                getClass().getSimpleName(), jdbcConnection);

        try {
            try (Connection connection = jdbcConnection.openConnection()) {
                try (var database = getLiquiDatabase(connection)) {
                    dropLiquibase(database, locations);
                }
            }
        } catch (Exception e) {
            try {
                Thread.sleep(250);
                try (Connection connection = jdbcConnection.openConnection()) {
                    try (var database = getLiquiDatabase(connection)) {
                        dropLiquibase(database, locations);
                    }
                }
            } catch (Exception ex) {
                logger.error("Failed migration drop for engine '{}' for connection: {}",
                        getClass().getSimpleName(), jdbcConnection);

                throw new IllegalStateException(ex);
            }
        }

        logger.info("Finished migration dropping for engine '{}' for connection: {}",
                getClass().getSimpleName(), jdbcConnection);
    }

    private Database getLiquiDatabase(Connection connection) {
        try {
            var liquiConnection = new liquibase.database.jvm.JdbcConnection(connection);
            return DatabaseFactory.getInstance().findCorrectDatabaseImplementation(liquiConnection);
        } catch (DatabaseException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void close() {
        // do nothing
    }
}
