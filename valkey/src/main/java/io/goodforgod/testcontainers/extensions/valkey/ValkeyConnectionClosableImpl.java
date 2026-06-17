package io.goodforgod.testcontainers.extensions.valkey;

import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
final class ValkeyConnectionClosableImpl extends ValkeyConnectionImpl {

    ValkeyConnectionClosableImpl(Params params, Params network) {
        super(params, network);
    }

    @Override
    public void close() {
        super.stop();
    }
}
