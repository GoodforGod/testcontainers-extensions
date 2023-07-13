package io.goodforgod.testcontainers.extensions.jdbc;

public final class JdbcConnectionException extends RuntimeException {

    JdbcConnectionException(Throwable cause) {
        super(cause);
    }
}
