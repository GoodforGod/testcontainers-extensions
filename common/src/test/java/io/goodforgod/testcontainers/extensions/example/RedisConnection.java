package io.goodforgod.testcontainers.extensions.example;

import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.util.Optional;

/**
 * Describes active Redis connection of currently running {@link RedisContainer}
 */
public interface RedisConnection {

    /**
     * Redis connection parameters
     */
    interface Params {

        @NotNull
        URI uri();

        @NotNull
        String host();

        int port();

        String username();

        String password();

        int database();
    }

    /**
     * @return connection parameters to container
     */
    @NotNull
    Params params();

    /**
     * @return connection parameters inside docker network, can be useful when one container require
     *             params to connect to Redis Database container inside docker network
     */
    @NotNull
    Optional<Params> paramsInNetwork();

    void deleteAll();
}
