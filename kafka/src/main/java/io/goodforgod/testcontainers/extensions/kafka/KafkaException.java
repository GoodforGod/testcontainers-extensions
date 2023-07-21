package io.goodforgod.testcontainers.extensions.kafka;

public final class KafkaException extends RuntimeException {

    KafkaException(String message) {
        super(message);
    }

    KafkaException(String message, Throwable cause) {
        super(message, cause);
    }
}
