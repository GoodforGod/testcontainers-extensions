package io.goodforgod.testcontainers.extensions.arangodb;

public final class ArangoConnectionException extends RuntimeException {

    public ArangoConnectionException(Throwable cause) {
        super(cause);
    }
}
