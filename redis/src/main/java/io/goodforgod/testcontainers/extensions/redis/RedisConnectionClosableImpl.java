package io.goodforgod.testcontainers.extensions.redis;

import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
final class RedisConnectionClosableImpl extends RedisConnectionImpl {

    RedisConnectionClosableImpl(Params params, Params network) {
        super(params, network);
    }

    @Override
    public void close() {
        super.stop();
    }
}
