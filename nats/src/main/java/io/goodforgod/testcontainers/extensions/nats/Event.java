package io.goodforgod.testcontainers.extensions.nats;

import io.nats.client.Message;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Nats Event constructed from {@link Message}
 */
public interface Event {

    /**
     * Nats Event Value
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
     * Nats Event Header
     */
    interface Header {

        @NotNull
        String key();

        @NotNull
        Value value();
    }

    /**
     * @return nats event value
     */
    @NotNull
    Value value();

    /**
     * @return nats event headers
     */
    @NotNull
    List<Header> headers();

    /**
     * @return nats event builder that can be used further for
     *             {@link NatsConnection#send(String, List)}
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
        return new EventImpl(new EventImpl.ValueImpl(valueAsBytes), Collections.emptyList());
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

    interface Builder {

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
         * @return nats event
         */
        @NotNull
        Event build();
    }
}
