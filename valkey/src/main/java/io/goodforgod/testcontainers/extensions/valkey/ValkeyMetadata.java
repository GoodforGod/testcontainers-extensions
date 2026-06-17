package io.goodforgod.testcontainers.extensions.valkey;

import io.goodforgod.testcontainers.extensions.AbstractContainerMetadata;
import io.goodforgod.testcontainers.extensions.ContainerMode;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
final class ValkeyMetadata extends AbstractContainerMetadata {

    ValkeyMetadata(boolean network, String alias, String image, ContainerMode runMode) {
        super(network, alias, image, runMode);
    }
}

