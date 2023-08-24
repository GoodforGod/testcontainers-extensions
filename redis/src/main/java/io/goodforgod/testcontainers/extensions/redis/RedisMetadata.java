package io.goodforgod.testcontainers.extensions.redis;

import io.goodforgod.testcontainers.extensions.AbstractContainerMetadata;
import io.goodforgod.testcontainers.extensions.ContainerMode;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;

@Internal
final class RedisMetadata extends AbstractContainerMetadata {

    RedisMetadata(boolean network, String alias, String image, ContainerMode runMode) {
        super(network, alias, image, runMode);
    }

    @Override
    protected @NotNull String networkAliasDefault() {
        return "redis-" + System.currentTimeMillis();
    }
}
