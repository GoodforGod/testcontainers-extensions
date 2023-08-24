package io.goodforgod.testcontainers.extensions.jdbc;

import io.goodforgod.testcontainers.extensions.ContainerMode;
import org.jetbrains.annotations.NotNull;

final class PostgresJdbcMetadata extends JdbcMetadata {

    PostgresJdbcMetadata(boolean network, String alias, String image, ContainerMode runMode, Migration migration) {
        super(network, alias, image, runMode, migration);
    }

    @Override
    protected @NotNull String networkAliasDefault() {
        return "postgres-" + System.currentTimeMillis();
    }
}
