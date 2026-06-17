package io.goodforgod.testcontainers.extensions.rabbitmq;

import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
final class RabbitMQConnectionException extends RuntimeException {

    RabbitMQConnectionException(String message) {
        super(message);
    }

    RabbitMQConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
}
