package io.goodforgod.testcontainers.extensions.redis;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;

/**
 * Describes active Redis connection of currently running {@link RedisContainerExtra}
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

    /**
     * @return new Redis connection
     */
    @NotNull
    RedisCommands commands();

    void deleteAll();

    /**
     * @param keyPrefix my-key-prefix
     * @return SELECT COUNT(*) from specified table
     */
    int countPrefix(@NotNull RedisKey keyPrefix);

    /**
     * @param keys to check for
     * @return SELECT COUNT(*) from specified table
     */
    int count(@NotNull RedisKey... keys);

    /**
     * @param keys to check for
     * @return SELECT COUNT(*) from specified table
     */
    int count(@NotNull Collection<RedisKey> keys);

    /**
     * Asserts that keys with prefix counts 0 values
     *
     * @param keyPrefix my-key-prefix
     */
    void assertCountsPrefixNone(@NotNull RedisKey keyPrefix);

    /**
     * Asserts that keys counts 0 values
     *
     * @param keys to check for
     */
    void assertCountsNone(@NotNull RedisKey... keys);

    /**
     * Asserts that keys counts 0 values
     *
     * @param keys to check for
     */
    void assertCountsNone(@NotNull Collection<RedisKey> keys);

    /**
     * Asserts that keys with prefix counts at least minimal number expectedAtLeast
     *
     * @param keyPrefix       my-key-prefix
     * @param expectedAtLeast at least minimal number of values expected
     */
    List<RedisValue> assertCountsPrefixAtLeast(long expectedAtLeast, @NotNull RedisKey keyPrefix);

    /**
     * Asserts that keys with prefix counts exact number expected values
     *
     * @param keyPrefix my-key-prefix
     * @param expected  exact number of values expected
     */
    List<RedisValue> assertCountsPrefixEquals(long expected, @NotNull RedisKey keyPrefix);

    /**
     * Asserts that keys at least minimal number expectedAtLeast
     *
     * @param keys            to check for
     * @param expectedAtLeast at least minimal number of values expected
     */
    List<RedisValue> assertCountsAtLeast(long expectedAtLeast, @NotNull RedisKey... keys);

    /**
     * Asserts that keys counts at least minimal number expectedAtLeast
     *
     * @param keys            to check for
     * @param expectedAtLeast at least minimal number of values expected
     */
    List<RedisValue> assertCountsAtLeast(long expectedAtLeast, @NotNull Collection<RedisKey> keys);

    /**
     * Asserts that keys counts exact number expected values
     *
     * @param keys     to check for
     * @param expected exact number of values expected
     */
    List<RedisValue> assertCountsEquals(long expected, @NotNull RedisKey... keys);

    /**
     * Asserts that keys counts exact number expected values
     *
     * @param keys     to check for
     * @param expected exact number of values expected
     */
    List<RedisValue> assertCountsEquals(long expected, @NotNull Collection<RedisKey> keys);
}
