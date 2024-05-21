package io.goodforgod.testcontainers.extensions.cassandra;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.cognitor.cassandra.migration.Database;
import org.cognitor.cassandra.migration.MigrationConfiguration;
import org.cognitor.cassandra.migration.MigrationRepository;
import org.cognitor.cassandra.migration.MigrationTask;
import org.jetbrains.annotations.NotNull;

public final class CognitorCassandraMigrationEngine extends AbstractDropCassandraMigrationEngine {

    public CognitorCassandraMigrationEngine(CassandraConnection connection) {
        super(connection);
    }

    @Override
    public void apply(@NotNull List<String> locations) {
        if (locations.isEmpty()) {
            logger.warn("Empty locations for schema migration for engine '{}' for connection: {}",
                    getClass().getSimpleName(), connection);
            return;
        }

        logger.debug("Starting schema migration for engine '{}' for connection: {}",
                getClass().getSimpleName(), connection);

        final List<String> validLocations = locations.stream()
                .filter(Objects::nonNull)
                .filter(location -> !location.isBlank())
                .distinct()
                .collect(Collectors.toList());

        if (validLocations.isEmpty()) {
            throw new IllegalArgumentException("Found 0 valid migration paths: " + locations);
        }

        Database database = new Database(connection.getConnection(),
                new MigrationConfiguration()
                        .withKeyspaceName(connection.params().keyspace()));
        MigrationTask migration = new MigrationTask(database, new MigrationRepository(validLocations));
        migration.migrate();

        logger.info("Finished schema migration for engine '{}' for connection: {}",
                getClass().getSimpleName(), connection);
    }
}
