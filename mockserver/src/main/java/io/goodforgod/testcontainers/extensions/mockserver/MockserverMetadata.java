package io.goodforgod.testcontainers.extensions.mockserver;

import io.goodforgod.testcontainers.extensions.AbstractContainerMetadata;
import io.goodforgod.testcontainers.extensions.ContainerMode;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;

@Internal
final class MockserverMetadata extends AbstractContainerMetadata {

    MockserverMetadata(boolean network, String alias, String image, ContainerMode runMode) {
        super(network, alias, image, runMode);
    }

    @Override
    public @NotNull String networkAliasDefault() {
        return "mockserver-" + System.currentTimeMillis();
    }
}
