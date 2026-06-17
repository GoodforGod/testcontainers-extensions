package io.goodforgod.testcontainers.extensions.valkey;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

/**
 * Valkey key
 */
public interface ValkeyKey {

    byte[] asBytes();

    @NotNull
    String asString();

    @NotNull
    static ValkeyKey of(byte[] key) {
        return new ValkeyKeyImpl(key);
    }

    @NotNull
    static List<ValkeyKey> of(byte[]... keys) {
        return Arrays.stream(keys)
                .map(ValkeyKey::of)
                .collect(Collectors.toList());
    }

    @NotNull
    static ValkeyKey of(@NotNull String key) {
        return of(key.getBytes(StandardCharsets.UTF_8));
    }

    @NotNull
    static List<ValkeyKey> of(@NotNull String... keys) {
        return Arrays.stream(keys)
                .map(ValkeyKey::of)
                .collect(Collectors.toList());
    }
}
