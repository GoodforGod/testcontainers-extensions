package io.goodforgod.testcontainers.extensions.cassandra;

import java.util.List;
import org.jetbrains.annotations.NotNull;

public interface CassandraMigrationEngine {

    void migrate(@NotNull List<String> locations);

    void drop(@NotNull List<String> locations);
}
