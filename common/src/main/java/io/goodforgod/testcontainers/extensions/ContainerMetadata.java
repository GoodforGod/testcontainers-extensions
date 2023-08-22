package io.goodforgod.testcontainers.extensions;

import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;

@Internal
public interface ContainerMetadata {

    /**
     * @see org.testcontainers.containers.Network#SHARED
     */
    boolean useNetworkShared();

    @NotNull
    String image();

    @NotNull
    ContainerMode runMode();
}
