package io.goodforgod.testcontainers.extensions.redis;

public final class RedisConnectionException extends RuntimeException {

    RedisConnectionException(Throwable cause) {
        super(cause);
    }
}
