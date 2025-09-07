package io.goodforgod.testcontainers.extensions.scylla;

import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
final class ScyllaConnectionClosableImpl extends ScyllaConnectionImpl {

    ScyllaConnectionClosableImpl(Params params, Params network) {
        super(params, network);
    }

    @Override
    public void close() {
        super.stop();
    }
}
