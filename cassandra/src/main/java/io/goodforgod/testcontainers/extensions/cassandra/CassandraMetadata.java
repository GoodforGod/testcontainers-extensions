package io.goodforgod.testcontainers.extensions.cassandra;

import io.goodforgod.testcontainers.extensions.ContainerMetadata;
import io.goodforgod.testcontainers.extensions.ContainerMode;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;

@Internal
final class CassandraMetadata implements ContainerMetadata {

    private final boolean network;
    private final String image;
    private final ContainerMode runMode;
    private final Migration migration;

    CassandraMetadata(boolean network, String image, ContainerMode runMode, Migration migration) {
        this.network = network;
        this.image = image;
        this.runMode = runMode;
        this.migration = migration;
    }

    @Override
    public boolean useNetworkShared() {
        return network;
    }

    public @NotNull String image() {
        return image;
    }

    public @NotNull ContainerMode runMode() {
        return runMode;
    }

    Migration migration() {
        return migration;
    }
}
