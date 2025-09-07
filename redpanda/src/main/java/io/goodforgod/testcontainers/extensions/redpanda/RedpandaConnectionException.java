package io.goodforgod.testcontainers.extensions.redpanda;

public final class RedpandaConnectionException extends RuntimeException {

    RedpandaConnectionException(String message) {
        super(message);
    }

    RedpandaConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
}
