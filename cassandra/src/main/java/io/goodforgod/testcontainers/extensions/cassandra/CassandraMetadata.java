package io.goodforgod.testcontainers.extensions.cassandra;

import io.goodforgod.testcontainers.extensions.AbstractContainerMetadata;
import io.goodforgod.testcontainers.extensions.ContainerMode;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;

@Internal
final class CassandraMetadata extends AbstractContainerMetadata {

    private final Migration migration;

    CassandraMetadata(boolean network, String alias, String image, ContainerMode runMode, Migration migration) {
        super(network, alias, image, runMode);
        this.migration = migration;
    }

    @Override
    public @NotNull String networkAliasDefault() {
        return "cassandra-" + System.currentTimeMillis();
    }

    Migration migration() {
        return migration;
    }
}
