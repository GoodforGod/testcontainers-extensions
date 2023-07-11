package io.goodforgod.testcontainers.extensions.sql;

public final class SqlConnectionException extends RuntimeException {

    public SqlConnectionException(Throwable cause) {
        super(cause);
    }
}
