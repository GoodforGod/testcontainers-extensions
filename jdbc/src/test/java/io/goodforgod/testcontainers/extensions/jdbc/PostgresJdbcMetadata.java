package io.goodforgod.testcontainers.extensions.jdbc;

import io.goodforgod.testcontainers.extensions.ContainerMode;

public final class PostgresJdbcMetadata extends JdbcMetadata {

    PostgresJdbcMetadata(boolean network, String alias, String image, ContainerMode runMode, Migration migration) {
        super(network, alias, image, runMode, migration);
    }
}
