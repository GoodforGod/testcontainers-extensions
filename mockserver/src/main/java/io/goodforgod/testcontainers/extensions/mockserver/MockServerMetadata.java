package io.goodforgod.testcontainers.extensions.mockserver;

import io.goodforgod.testcontainers.extensions.AbstractContainerMetadata;
import io.goodforgod.testcontainers.extensions.ContainerMode;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
final class MockServerMetadata extends AbstractContainerMetadata {

    MockServerMetadata(boolean network, String alias, String image, ContainerMode runMode) {
        super(network, alias, image, runMode);
    }
}
