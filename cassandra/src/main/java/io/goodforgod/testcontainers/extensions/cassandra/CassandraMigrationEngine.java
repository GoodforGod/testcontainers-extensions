package io.goodforgod.testcontainers.extensions.cassandra;

import java.util.List;
import org.jetbrains.annotations.NotNull;

public interface CassandraMigrationEngine {

    default void apply(@NotNull String location) {
        apply(List.of(location));
    }

    void apply(@NotNull List<String> locations);

    default void drop(@NotNull String location, Migration.DropMode dropMode) {
        drop(List.of(location), dropMode);
    }

    void drop(@NotNull List<String> locations, Migration.DropMode dropMode);
}
