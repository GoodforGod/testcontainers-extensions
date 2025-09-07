package io.goodforgod.testcontainers.extensions.redpanda;

import java.util.*;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

@Internal
final class RedpandaConnectionClosableImpl extends RedpandaConnectionImpl {

    RedpandaConnectionClosableImpl(Properties properties, @Nullable Properties propertiesInNetwork) {
        super(properties, propertiesInNetwork);
    }

    @Override
    public void close() {
        super.stop();
    }
}
