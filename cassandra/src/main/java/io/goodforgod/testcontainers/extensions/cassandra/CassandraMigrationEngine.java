package io.goodforgod.testcontainers.extensions.cassandra;

import java.util.List;
import org.jetbrains.annotations.NotNull;

public interface CassandraMigrationEngine {

    default void apply(@NotNull String location) {
        apply(List.of(location));
    }

    void apply(@NotNull List<String> locations);

    default void drop(@NotNull String location) {
        drop(List.of(location));
    }

    void drop(@NotNull List<String> locations);
}
