package io.goodforgod.testcontainers.extensions.jdbc;

import java.util.List;
import org.jetbrains.annotations.NotNull;

public interface JdbcMigrationEngine {

    void migrate(@NotNull JdbcConnection connection, @NotNull List<String> locations);

    void drop(@NotNull JdbcConnection connection, @NotNull List<String> locations);
}
