package io.goodforgod.testcontainers.extensions.cassandra;

import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
final class CassandraConnectionClosableImpl extends CassandraConnectionImpl {

    CassandraConnectionClosableImpl(Params params, Params network) {
        super(params, network);
    }

    @Override
    public void close() {
        super.stop();
    }
}
