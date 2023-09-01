package io.goodforgod.testcontainers.extensions.redis;

import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;

@Internal
final class RedisValueImpl implements RedisValue {

    private final byte[] value;

    RedisValueImpl(byte[] value) {
        this.value = value;
    }

    @Override
    public byte[] asBytes() {
        return value;
    }

    @Override
    public @NotNull String asString() {
        return new String(value, StandardCharsets.UTF_8);
    }

    @Override
    public @NotNull JSONObject asJson() {
        return new JSONObject(asString());
    }

    @Override
    public @NotNull JSONArray asJsonArray() {
        return new JSONArray(asString());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        RedisValueImpl value1 = (RedisValueImpl) o;
        return Objects.equals(asString(), value1.asString());
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(value);
    }

    @Override
    public String toString() {
        return asString();
    }
}
