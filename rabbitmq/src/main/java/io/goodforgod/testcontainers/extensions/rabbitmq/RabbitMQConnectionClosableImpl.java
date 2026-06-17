package io.goodforgod.testcontainers.extensions.rabbitmq;

import java.util.Properties;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

@Internal
final class RabbitMQConnectionClosableImpl extends RabbitMQConnectionImpl {

    RabbitMQConnectionClosableImpl(Properties properties, @Nullable Properties propertiesInNetwork) {
        super(properties, propertiesInNetwork);
    }

    @Override
    public void close() {
        super.stop();
    }
}