package io.goodforgod.testcontainers.extensions.valkey;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

@Internal
final class ValkeyValueImpl implements ValkeyValue {

    private final byte[] value;

    ValkeyValueImpl(byte[] value) {
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
        ValkeyValueImpl value1 = (ValkeyValueImpl) o;
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
