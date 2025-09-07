package io.goodforgod.testcontainers.extensions.redpanda;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Redpanda Event constructed from {@link org.apache.kafka.clients.consumer.ConsumerRecord}
 */
public interface Event {

    /**
     * Redpanda Event key
     */
    interface Key {

        byte[] asBytes();

        @NotNull
        String asString();
    }

    /**
     * Redpanda Event Value
     */
    interface Value {

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

    /**
     * Redpanda Event Header
     */
    interface Header {

        @NotNull
        String key();

        @NotNull
        Value value();
    }

    /**
     * @return The key (or null if no key is specified)
     */
    Key key();

    /**
     * @return redpanda event value
     */
    @NotNull
    Value value();

    /**
     * @return redpanda event headers
     */
    @NotNull
    List<Header> headers();

    /**
     * @return redpanda event builder that can be used further for
     *             {@link RedpandaConnection#send(String, List)}
     */
    @NotNull
    static Builder builder() {
        return new EventImpl.EventBuilder();
    }

    /**
     * @param valueAsBytes for {@link Event#value()}
     * @return event with value only
     */
    @NotNull
    static Event ofValue(byte[] valueAsBytes) {
        return new EventImpl(null, new EventImpl.ValueImpl(valueAsBytes), Collections.emptyList());
    }

    /**
     * @param valueAsString for {@link Event#value()}
     * @return event with value only
     */
    @NotNull
    static Event ofValue(@NotNull String valueAsString) {
        return ofValue(valueAsString.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * JSON <a href="https://www.baeldung.com/java-org-json">JSON</a>
     * 
     * @param valueAsJson for {@link Event#value()}
     * @return event with value only
     */
    @NotNull
    static Event ofValue(@NotNull JSONObject valueAsJson) {
        return ofValue(valueAsJson.toString().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * JSON <a href="https://www.baeldung.com/java-org-json">JSON</a>
     * 
     * @param valueAsJsonArray for {@link Event#value()}
     * @return event with value only
     */
    @NotNull
    static Event ofValue(@NotNull JSONArray valueAsJsonArray) {
        return ofValue(valueAsJsonArray.toString().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * @param valueAsBytes for {@link Event#value()}
     * @return event with value and random key for {@link Event#key()}
     */
    @NotNull
    static Event ofValueAndRandomKey(byte[] valueAsBytes) {
        return new EventImpl(new EventImpl.KeyImpl(UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8)),
                new EventImpl.ValueImpl(valueAsBytes),
                Collections.emptyList());
    }

    /**
     * @param valueAsString for {@link Event#value()}
     * @return event with value and random key for {@link Event#key()}
     */
    @NotNull
    static Event ofValueAndRandomKey(@NotNull String valueAsString) {
        return ofValueAndRandomKey(valueAsString.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * JSON <a href="https://www.baeldung.com/java-org-json">JSON</a>
     * 
     * @param valueAsJson for {@link Event#value()}
     * @return event with value and random key for {@link Event#key()}
     */
    @NotNull
    static Event ofValueAndRandomKey(@NotNull JSONObject valueAsJson) {
        return ofValueAndRandomKey(valueAsJson.toString().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * JSON <a href="https://www.baeldung.com/java-org-json">JSON</a>
     * 
     * @param valueAsJsonArray for {@link Event#value()}
     * @return event with value and random key for {@link Event#key()}
     */
    @NotNull
    static Event ofValueAndRandomKey(@NotNull JSONArray valueAsJsonArray) {
        return ofValueAndRandomKey(valueAsJsonArray.toString().getBytes(StandardCharsets.UTF_8));
    }

    interface Builder {

        @NotNull
        Builder withKey(@NotNull String key);

        @NotNull
        Builder withKey(byte[] key);

        @NotNull
        Builder withValue(byte[] value);

        @NotNull
        Builder withValue(@NotNull String value);

        /**
         * @param value JSON <a href="https://www.baeldung.com/java-org-json">JSON</a>
         * @return self
         */
        @NotNull
        Builder withValue(@NotNull JSONObject value);

        /**
         * @param value JSON <a href="https://www.baeldung.com/java-org-json">JSON</a>
         * @return self
         */
        @NotNull
        Builder withValue(@NotNull JSONArray value);

        @NotNull
        Builder withHeader(@NotNull String key, byte[] value);

        @NotNull
        Builder withHeader(@NotNull String key, @NotNull String value);

        /**
         * @return redpanda event
         */
        @NotNull
        Event build();
    }
}
