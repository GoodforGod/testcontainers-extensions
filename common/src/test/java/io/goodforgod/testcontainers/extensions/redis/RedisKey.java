package io.goodforgod.testcontainers.extensions.redis;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

/**
 * Redis key
 */
public interface RedisKey {

    byte[] asBytes();

    @NotNull
    String asString();

    @NotNull
    static RedisKey of(byte[] key) {
        return new RedisKeyImpl(key);
    }

    /**
     * @param keys multiple keys as bytes
     * @return List of RedisKeys
     */
    @NotNull
    static List<RedisKey> of(byte[]... keys) {
        return Arrays.stream(keys)
                .map(RedisKey::of)
                .collect(Collectors.toList());
    }

    /**
     * @param key value
     * @return RedisValue with value only
     */
    @NotNull
    static RedisKey of(@NotNull String key) {
        return of(key.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * @param keys multiple keys as bytes
     * @return List of RedisKeys
     */
    @NotNull
    static List<RedisKey> of(@NotNull String... keys) {
        return Arrays.stream(keys)
                .map(RedisKey::of)
                .collect(Collectors.toList());
    }
}
