package io.goodforgod.testcontainers.extensions.jdbc;

import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
final class ContainerMetadata {

    private final String image;
    private final ContainerMode runMode;
    private final Migration migration;

    ContainerMetadata(String image, ContainerMode runMode, Migration migration) {
        this.image = image;
        this.runMode = runMode;
        this.migration = migration;
    }

    String image() {
        return image;
    }

    ContainerMode runMode() {
        return runMode;
    }

    Migration migration() {
        return migration;
    }
}
