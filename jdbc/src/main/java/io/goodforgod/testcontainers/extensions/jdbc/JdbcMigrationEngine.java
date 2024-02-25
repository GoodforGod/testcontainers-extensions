package io.goodforgod.testcontainers.extensions.jdbc;

import java.util.List;
import org.jetbrains.annotations.NotNull;

public interface JdbcMigrationEngine extends AutoCloseable {

    void migrate(@NotNull List<String> locations);

    void drop(@NotNull List<String> locations);
}
