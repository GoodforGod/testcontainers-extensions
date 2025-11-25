package io.goodforgod.testcontainers.extensions.nats;

import java.util.*;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

@Internal
final class NatsConnectionClosableImpl extends NatsConnectionImpl {

    NatsConnectionClosableImpl(Properties properties, @Nullable Properties propertiesInNetwork) {
        super(properties, propertiesInNetwork);
    }

    @Override
    public void close() {
        super.stop();
    }
}
