package io.goodforgod.testcontainers.extensions.kafka;

public final class KafkaConnectionException extends RuntimeException {

    KafkaConnectionException(String message) {
        super(message);
    }

    KafkaConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
}
