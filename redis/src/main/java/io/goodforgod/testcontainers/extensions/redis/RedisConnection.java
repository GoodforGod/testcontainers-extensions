package io.goodforgod.testcontainers.extensions.redis;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import redis.clients.jedis.Jedis;

/**
 * Describes active Redis connection of currently running {@link RedisContainer}
 */
public interface RedisConnection {

    /**
     * Redis connection parameters
     */
    interface Params {

        @NotNull
        String host();

        int port();

        String username();

        String password();
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
    Jedis get();

    void deleteAll();

    /**
     * @param keyPrefix my-key-prefix
     * @return SELECT COUNT(*) from specified table
     */
    int countPrefix(@NotNull String keyPrefix);

    /**
     * @param keys to check for
     * @return SELECT COUNT(*) from specified table
     */
    int count(@NotNull String... keys);

    /**
     * @param keys to check for
     * @return SELECT COUNT(*) from specified table
     */
    int count(@NotNull Collection<String> keys);

    /**
     * Asserts that keys with prefix counts 0 values
     *
     * @param keyPrefix my-key-prefix
     */
    void assertCountsPrefixNone(@NotNull String keyPrefix);

    /**
     * Asserts that keys counts 0 values
     *
     * @param keys to check for
     */
    void assertCountsNone(@NotNull String... keys);

    /**
     * Asserts that keys counts 0 values
     *
     * @param keys to check for
     */
    void assertCountsNone(@NotNull Collection<String> keys);

    /**
     * Asserts that keys with prefix counts at least minimal number expectedAtLeast
     *
     * @param keyPrefix       my-key-prefix
     * @param expectedAtLeast at least minimal number of values expected
     */
    Map<String, String> assertCountsPrefixAtLeast(long expectedAtLeast, @NotNull String keyPrefix);

    /**
     * Asserts that keys with prefix counts exact number expected values
     *
     * @param keyPrefix my-key-prefix
     * @param expected  exact number of values expected
     */
    Map<String, String> assertCountsPrefixEquals(long expected, @NotNull String keyPrefix);

    /**
     * Asserts that keys at least minimal number expectedAtLeast
     *
     * @param keys            to check for
     * @param expectedAtLeast at least minimal number of values expected
     */
    List<String> assertCountsAtLeast(long expectedAtLeast, @NotNull String... keys);

    /**
     * Asserts that keys counts exact number expected values
     *
     * @param keys     to check for
     * @param expected exact number of values expected
     */
    List<String> assertCountsEquals(long expected, @NotNull String... keys);

    /**
     * Asserts that keys counts at least minimal number expectedAtLeast
     *
     * @param keys            to check for
     * @param expectedAtLeast at least minimal number of values expected
     */
    List<String> assertCountsAtLeast(long expectedAtLeast, @NotNull Collection<String> keys);

    /**
     * Asserts that keys counts exact number expected values
     *
     * @param keys     to check for
     * @param expected exact number of values expected
     */
    List<String> assertCountsEquals(long expected, @NotNull Collection<String> keys);
}
