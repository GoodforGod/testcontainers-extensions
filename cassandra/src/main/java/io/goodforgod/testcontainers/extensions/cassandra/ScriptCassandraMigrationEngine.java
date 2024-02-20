package io.goodforgod.testcontainers.extensions.cassandra;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ScriptCassandraMigrationEngine implements CassandraMigrationEngine {

    private static final Logger logger = LoggerFactory.getLogger(ScriptCassandraMigrationEngine.class);

    private static class Table {

        private final String keyspace;
        private final String name;

        private Table(String keyspace, String name) {
            this.keyspace = keyspace;
            this.name = name;
        }

        public String keyspace() {
            return keyspace;
        }

        public String name() {
            return name;
        }
    }

    private final CassandraConnection connection;

    public ScriptCassandraMigrationEngine(CassandraConnection connection) {
        this.connection = connection;
    }

    private static List<File> getFilesFromLocations(List<String> locations) {
        final ClassLoader loader = Thread.currentThread().getContextClassLoader();
        return locations.stream()
                .flatMap(location -> {
                    final URL url = loader.getResource(location);
                    final String path = url.getPath();
                    final File file = new File(path);
                    return file.isFile()
                            ? Stream.of(file)
                            : Arrays.stream(file.listFiles()).sorted();
                })
                .collect(Collectors.toList());
    }

    @Override
    public void migrate(@NotNull List<String> locations) {
        if (locations.isEmpty()) {
            logger.warn("Empty locations for schema migration for engine '{}' for connection: {}",
                    getClass().getSimpleName(), connection);
            return;
        }

        logger.debug("Starting schema migration for engine '{}' for connection: {}",
                getClass().getSimpleName(), connection);

        final Set<String> validLocations = locations.stream()
                .filter(Objects::nonNull)
                .filter(location -> !location.isBlank())
                .collect(Collectors.toSet());

        if (validLocations.isEmpty()) {
            throw new IllegalArgumentException("Found 0 valid migration paths: " + locations);
        }

        final List<File> filesToUseForMigration = getFilesFromLocations(locations);
        for (File file : filesToUseForMigration) {
            try {
                final String cql = Files.readString(file.toPath());
                final List<String> queries = Arrays.stream(cql.split(";"))
                        .map(query -> query + ";")
                        .collect(Collectors.toList());

                for (String query : queries) {
                    connection.execute(query);
                }
            } catch (IOException e) {
                throw new IllegalArgumentException("Illegal file for migration: " + file.getPath(), e);
            }
        }

        logger.debug("Finished schema migration for engine '{}' for connection: {}",
                getClass().getSimpleName(), connection);
    }

    @Override
    public void drop(@NotNull List<String> locations) {
        if (locations.isEmpty()) {
            logger.warn("Empty locations for schema migration for engine '{}' for connection: {}",
                    getClass().getSimpleName(), connection);
            return;
        }

        logger.debug("Starting schema dropping for engine '{}' for connection: {}",
                getClass().getSimpleName(), connection);

        var tables = ((CassandraConnectionImpl) connection).queryMany(
                "SELECT keyspace_name, table_name FROM system_schema.tables;",
                r -> new Table(r.getString(0), r.getString(1)));

        for (Table table : tables) {
            if (!table.keyspace().startsWith("system")) {
                connection.execute("TRUNCATE TABLE " + table.keyspace() + "." + table.name());
            }
        }

        logger.debug("Finished schema dropping for engine '{}' for connection: {}",
                getClass().getSimpleName(), connection);
    }
}
