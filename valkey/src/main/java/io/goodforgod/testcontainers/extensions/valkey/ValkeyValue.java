package io.goodforgod.testcontainers.extensions.valkey;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Valkey value
 */
public interface ValkeyValue {

    byte[] asBytes();

    @NotNull
    String asString();

    @NotNull
    JSONObject asJson();

    @NotNull
    JSONArray asJsonArray();
}
