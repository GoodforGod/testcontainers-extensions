package io.goodforgod.testcontainers.extensions.valkey;

public final class ValkeyConnectionException extends RuntimeException {

    ValkeyConnectionException(Throwable cause) {
        super(cause);
    }
}

