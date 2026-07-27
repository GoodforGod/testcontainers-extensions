package io.goodforgod.testcontainers.extensions.redis;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Redis Value
 */
public interface RedisValue {

    byte[] asBytes();

    @NotNull
    String asString();

    /**
     * <a href="https://www.baeldung.com/java-org-json">JSON</a>
     *
     * @return as json object
     */
    @NotNull
    JSONObject asJson();

    /**
     * <a href="https://www.baeldung.com/java-org-json">JSON</a>
     *
     * @return as json object
     */
    @NotNull
    JSONArray asJsonArray();
}
