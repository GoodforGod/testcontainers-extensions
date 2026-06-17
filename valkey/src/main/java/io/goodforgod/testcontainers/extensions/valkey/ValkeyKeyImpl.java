package io.goodforgod.testcontainers.extensions.valkey;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;

@Internal
final class ValkeyKeyImpl implements ValkeyKey {

    private final byte[] key;

    ValkeyKeyImpl(byte[] key) {
        this.key = key;
    }

    @Override
    public byte[] asBytes() {
        return key;
    }

    @Override
    public @NotNull String asString() {
        return new String(key, StandardCharsets.UTF_8);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ValkeyKeyImpl key1 = (ValkeyKeyImpl) o;
        return Objects.equals(asString(), key1.asString());
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(key);
    }

    @Override
    public String toString() {
        return asString();
    }
}
