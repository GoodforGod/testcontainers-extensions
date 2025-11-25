package io.goodforgod.testcontainers.extensions.nats;

public final class NatsConnectionException extends RuntimeException {

    NatsConnectionException(String message) {
        super(message);
    }

    NatsConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
}
