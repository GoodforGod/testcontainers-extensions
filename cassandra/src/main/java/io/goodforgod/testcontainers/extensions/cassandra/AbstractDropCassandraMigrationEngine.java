package io.goodforgod.testcontainers.extensions.cassandra;

import java.util.*;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class AbstractDropCassandraMigrationEngine implements CassandraMigrationEngine {

    private static final Set<String> SYSTEM_KEYSPACES = Set.of("system", "system_auth", "system_schema", "system_distributed",
            "system_traces");

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private record Table(String keyspace, String name) {}

    protected final CassandraConnection connection;

    public AbstractDropCassandraMigrationEngine(CassandraConnection connection) {
        this.connection = connection;
    }

    @Override
    public void drop(@NotNull List<String> locations, Migration.DropMode mode) {
        if (locations.isEmpty()) {
            logger.warn("Empty locations for schema migration for engine '{}' for connection: {}",
                    getClass().getSimpleName(), connection);
            return;
        }

        logger.debug("Starting schema dropping for engine '{}' for connection: {}",
                getClass().getSimpleName(), connection);

        var tables = connection.queryMany(
                "SELECT keyspace_name, table_name FROM system_schema.tables;",
                r -> new Table(r.getString(0), r.getString(1)));

        for (Table table : tables) {
            if (table.keyspace().equals(connection.params().keyspace()) && !SYSTEM_KEYSPACES.contains(table.keyspace())) {
                // always try to use TRUNCATE cause DROP is SUPER slow, drop keyspace is even slower
                if (mode == Migration.DropMode.TRUNCATE) {
                    connection.execute("TRUNCATE TABLE " + table.keyspace() + "." + table.name());
                } else {
                    connection.execute("DROP TABLE " + table.keyspace() + "." + table.name());
                }
            }
        }

        logger.info("Finished schema dropping for engine '{}' for connection: {}",
                getClass().getSimpleName(), connection);
    }
}
