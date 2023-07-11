package io.goodforgod.testcontainers.extensions.jdbc;

public final class JdbcConnectionException extends RuntimeException {

    public JdbcConnectionException(Throwable cause) {
        super(cause);
    }
}
