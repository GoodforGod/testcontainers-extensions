package io.goodforgod.testcontainers.extensions.kafka;

import java.util.*;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

@Internal
final class KafkaConnectionClosableImpl extends KafkaConnectionImpl {

    KafkaConnectionClosableImpl(Properties properties, @Nullable Properties propertiesInNetwork) {
        super(properties, propertiesInNetwork);
    }

    @Override
    public void close() {
        super.stop();
    }
}
