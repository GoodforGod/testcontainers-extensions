package io.goodforgod.testcontainers.extensions.jdbc;

import io.goodforgod.testcontainers.extensions.AbstractContainerMetadata;
import io.goodforgod.testcontainers.extensions.ContainerMode;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
final class JdbcMetadata extends AbstractContainerMetadata {

    private final Migration migration;

    JdbcMetadata(boolean network, String image, ContainerMode runMode, Migration migration) {
        super(network, image, runMode);
        this.migration = migration;
    }

    Migration migration() {
        return migration;
    }
}
