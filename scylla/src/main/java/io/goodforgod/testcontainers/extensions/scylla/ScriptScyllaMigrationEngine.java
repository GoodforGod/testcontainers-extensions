package io.goodforgod.testcontainers.extensions.scylla;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;

public final class ScriptScyllaMigrationEngine extends AbstractDropScyllaMigrationEngine {

    public ScriptScyllaMigrationEngine(ScyllaConnection connection) {
        super(connection);
    }

    private static List<File> getFilesFromLocations(List<String> locations) {
        final ClassLoader loader = Thread.currentThread().getContextClassLoader();
        return locations.stream()
                .flatMap(location -> {
                    final URL url = loader.getResource(location);
                    final String path = url.getPath();
                    final File file = new File(path);
                    return file.isDirectory()
                            ? Arrays.stream(file.listFiles()).sorted(Comparator.comparing(File::getName))
                            : Stream.of(file);
                })
                .collect(Collectors.toList());
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
                        .map(String::trim)
                        .filter(query -> !query.isBlank())
                        .map(query -> query.trim() + ";")
                        .collect(Collectors.toList());

                for (String query : queries) {
                    connection.execute(query);
                }
            } catch (IOException e) {
                throw new IllegalArgumentException("Illegal file for migration: " + file.getPath(), e);
            }
        }

        logger.info("Finished schema migration for engine '{}' for connection: {}",
                getClass().getSimpleName(), connection);
    }
}
