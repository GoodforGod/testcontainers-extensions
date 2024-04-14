package io.goodforgod.testcontainers.extensions.jdbc;

import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
final class JdbcConnectionClosableImpl extends JdbcConnectionImpl {

    JdbcConnectionClosableImpl(Params params, Params network) {
        super(params, network);
    }

    @Override
    public void close() {
        super.stop();
    }
}
