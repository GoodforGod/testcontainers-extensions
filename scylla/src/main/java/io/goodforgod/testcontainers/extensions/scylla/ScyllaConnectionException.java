package io.goodforgod.testcontainers.extensions.scylla;

public final class ScyllaConnectionException extends RuntimeException {

    ScyllaConnectionException(Throwable cause) {
        super(cause);
    }
}
