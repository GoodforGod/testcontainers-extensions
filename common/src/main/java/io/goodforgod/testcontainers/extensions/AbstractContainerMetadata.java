package io.goodforgod.testcontainers.extensions;

import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;

@Internal
public abstract class AbstractContainerMetadata implements ContainerMetadata {

    private final boolean network;
    private final String image;
    private final ContainerMode runMode;

    protected AbstractContainerMetadata(boolean network, String image, ContainerMode runMode) {
        this.network = network;
        this.image = image;
        this.runMode = runMode;
    }

    @Override
    public boolean useNetworkShared() {
        return network;
    }

    @Override
    public @NotNull String image() {
        return image;
    }

    @Override
    public @NotNull ContainerMode runMode() {
        return runMode;
    }
}
