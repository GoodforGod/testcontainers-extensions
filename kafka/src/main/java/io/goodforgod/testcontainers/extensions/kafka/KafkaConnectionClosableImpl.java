package io.goodforgod.testcontainers.extensions.kafka;

import java.util.*;
import java.util.concurrent.*;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

@Internal
final class KafkaConnectionClosableImpl extends KafkaConnectionImpl implements KafkaConnectionClosable {

    KafkaConnectionClosableImpl(Properties properties, @Nullable Properties propertiesInNetwork) {
        super(properties, propertiesInNetwork);
    }

    @Override
    public void close() {
        super.close();
    }
}
