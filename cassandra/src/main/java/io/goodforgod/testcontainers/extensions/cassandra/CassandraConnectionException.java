package io.goodforgod.testcontainers.extensions.cassandra;

public final class CassandraConnectionException extends RuntimeException {

    CassandraConnectionException(Throwable cause) {
        super(cause);
    }
}
