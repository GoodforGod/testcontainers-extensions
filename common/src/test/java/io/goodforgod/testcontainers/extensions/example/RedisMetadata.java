package io.goodforgod.testcontainers.extensions.example;

import io.goodforgod.testcontainers.extensions.AbstractContainerMetadata;
import io.goodforgod.testcontainers.extensions.ContainerMode;
import org.jetbrains.annotations.NotNull;

final class RedisMetadata extends AbstractContainerMetadata {

    RedisMetadata(boolean network, String alias, String image, ContainerMode runMode) {
        super(network, alias, image, runMode);
    }

    @Override
    public @NotNull String networkAliasDefault() {
        return "redis-" + System.currentTimeMillis();
    }
}
