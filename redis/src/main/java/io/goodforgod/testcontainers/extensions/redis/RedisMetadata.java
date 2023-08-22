package io.goodforgod.testcontainers.extensions.redis;

import io.goodforgod.testcontainers.extensions.ContainerMetadata;
import io.goodforgod.testcontainers.extensions.ContainerMode;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;

@Internal
final class RedisMetadata implements ContainerMetadata {

    private final boolean network;
    private final String image;
    private final ContainerMode runMode;

    RedisMetadata(boolean network, String image, ContainerMode runMode) {
        this.network = network;
        this.image = image;
        this.runMode = runMode;
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
}
