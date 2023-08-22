package io.goodforgod.testcontainers.extensions.jdbc;

import io.goodforgod.testcontainers.extensions.AbstractTestcontainersExtension;
import java.io.FileWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.util.*;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.DatabaseFactory;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.flywaydb.core.Flyway;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.junit.jupiter.api.extension.*;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.testcontainers.containers.JdbcDatabaseContainer;

@Internal
abstract class AbstractTestcontainersJdbcExtension<Container extends JdbcDatabaseContainer<?>> extends
        AbstractTestcontainersExtension<JdbcConnection, Container, JdbcMetadata> {

    private static volatile boolean isLiquibaseActivated = false;

    @Override
    protected Class<JdbcConnection> getConnectionType() {
        return JdbcConnection.class;
    }

    private static Flyway getFlyway(JdbcConnection connection, List<String> locations) {
        final List<String> migrationLocations = (locations.isEmpty())
                ? List.of("classpath:db/migration")
                : locations;

        return Flyway.configure()
                .loggers("slf4j")
                .dataSource(connection.params().jdbcUrl(), connection.params().username(), connection.params().password())
                .locations(migrationLocations.toArray(String[]::new))
                .cleanDisabled(false)
                .load();
    }

    private static void migrateFlyway(JdbcConnection connection, List<String> locations) {
        getFlyway(connection, locations).migrate();
    }

    private static void dropFlyway(JdbcConnection connection, List<String> locations) {
        getFlyway(connection, locations).clean();
    }

    @FunctionalInterface
    interface LiquibaseRunner {

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

    private static void dropLiquibase(JdbcConnection connection, List<String> locations) {
        prepareLiquibase(connection, locations, (liquibase, writer) -> liquibase.dropAll());
    }

    private void tryMigrateIfRequired(JdbcMetadata annotation, JdbcConnection jdbcConnection) {
        if (annotation.migration().engine() == Migration.Engines.FLYWAY) {
            logger.debug("Starting schema migration for engine '{}' for connection: {}",
                    annotation.migration().engine(), jdbcConnection);
            migrateFlyway(jdbcConnection, Arrays.asList(annotation.migration().migrations()));
            logger.debug("Finished schema migration for engine '{}' for connection: {}",
                    annotation.migration().engine(), jdbcConnection);
        } else if (annotation.migration().engine() == Migration.Engines.LIQUIBASE) {
            logger.debug("Starting schema migration for engine '{}' for connection: {}",
                    annotation.migration().engine(), jdbcConnection);
            migrateLiquibase(jdbcConnection, Arrays.asList(annotation.migration().migrations()));
            logger.debug("Finished schema migration for engine '{}' for connection: {}",
                    annotation.migration().engine(), jdbcConnection);
        }
    }

    private void tryDropIfRequired(JdbcMetadata annotation, JdbcConnection jdbcConnection) {
        if (annotation.migration().engine() == Migration.Engines.FLYWAY) {
            logger.debug("Starting schema dropping for engine '{}' for connection: {}", annotation.migration().engine(),
                    jdbcConnection);
            dropFlyway(jdbcConnection, Arrays.asList(annotation.migration().migrations()));
        } else if (annotation.migration().engine() == Migration.Engines.LIQUIBASE) {
            logger.debug("Starting schema dropping for engine '{}' for connection: {}", annotation.migration().engine(),
                    jdbcConnection);
            dropLiquibase(jdbcConnection, Arrays.asList(annotation.migration().migrations()));
        }
    }

    @Override
    public void beforeAll(ExtensionContext context) {
        super.beforeAll(context);

        var metadata = getMetadata(context);
        var connectionCurrent = getConnectionCurrent(context);
        if (metadata.migration().apply() == Migration.Mode.PER_CLASS) {
            tryMigrateIfRequired(metadata, connectionCurrent);
        }
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        super.beforeEach(context);

        var metadata = getMetadata(context);
        var connectionCurrent = getConnectionCurrent(context);
        if (metadata.migration().apply() == Migration.Mode.PER_METHOD) {
            tryMigrateIfRequired(metadata, connectionCurrent);
        }
    }

    @Override
    public void afterEach(ExtensionContext context) {
        var metadata = getMetadata(context);
        var connectionCurrent = getConnectionCurrent(context);
        if (metadata.migration().drop() == Migration.Mode.PER_METHOD) {
            tryDropIfRequired(metadata, connectionCurrent);
        }

        super.afterEach(context);
    }

    @Override
    public void afterAll(ExtensionContext context) {
        var metadata = getMetadata(context);
        var connectionCurrent = getConnectionCurrent(context);
        if (metadata.migration().drop() == Migration.Mode.PER_CLASS) {
            tryDropIfRequired(metadata, connectionCurrent);
        }

        super.afterAll(context);
    }
}
