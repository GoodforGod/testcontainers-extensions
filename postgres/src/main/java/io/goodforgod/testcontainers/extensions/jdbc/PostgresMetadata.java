package io.goodforgod.testcontainers.extensions.jdbc;

import io.goodforgod.testcontainers.extensions.ContainerMode;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;

@Internal
final class PostgresMetadata extends JdbcMetadata {

    public PostgresMetadata(boolean network, String alias, String image, ContainerMode runMode, Migration migration) {
        super(network, alias, image, runMode, migration);
    }

    @Override
    protected @NotNull String networkAliasDefault() {
        return "postgres-" + System.currentTimeMillis();
    }
}
