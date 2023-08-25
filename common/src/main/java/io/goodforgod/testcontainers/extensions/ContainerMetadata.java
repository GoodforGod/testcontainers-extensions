package io.goodforgod.testcontainers.extensions;

import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Internal
public interface ContainerMetadata {

    /**
     * @see Network
     */
    boolean networkShared();

    /**
     * @see Network
     */
    @Nullable
    String networkAlias();

    @NotNull
    String networkAliasDefault();

    /**
     * @see Network
     */
    @NotNull
    String networkAliasOrDefault();

    @NotNull
    String image();

    @NotNull
    ContainerMode runMode();
}
