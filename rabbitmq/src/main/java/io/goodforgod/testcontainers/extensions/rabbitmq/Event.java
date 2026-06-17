package io.goodforgod.testcontainers.extensions.rabbitmq;

import com.rabbitmq.client.Delivery;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * RabbitMQ Event constructed from {@link Delivery}
 */
public interface Event {

    interface Value {

        byte[] asBytes();

        @NotNull
        String asString();

        @NotNull
        JSONObject asJson();

        @NotNull
        JSONArray asJsonArray();
    }

    interface Header {

        @NotNull
        String key();

        @NotNull
        Value value();
    }

    @NotNull
    Value value();

    @NotNull
    List<Header> headers();

    @NotNull
    static Builder builder() {
        return new EventImpl.EventBuilder();
    }

    @NotNull
    static Event ofValue(byte[] valueAsBytes) {
        return new EventImpl(new EventImpl.ValueImpl(valueAsBytes), Collections.emptyList());
    }

    @NotNull
    static Event ofValue(@NotNull String valueAsString) {
        return ofValue(valueAsString.getBytes(StandardCharsets.UTF_8));
    }

    @NotNull
    static Event ofValue(@NotNull JSONObject valueAsJson) {
        return ofValue(valueAsJson.toString().getBytes(StandardCharsets.UTF_8));
    }

    @NotNull
    static Event ofValue(@NotNull JSONArray valueAsJsonArray) {
        return ofValue(valueAsJsonArray.toString().getBytes(StandardCharsets.UTF_8));
    }

    interface Builder {

        @NotNull
        Builder withValue(byte[] value);

        @NotNull
        Builder withValue(@NotNull String value);

        @NotNull
        Builder withValue(@NotNull JSONObject value);

        @NotNull
        Builder withValue(@NotNull JSONArray value);

        @NotNull
        Builder withHeader(@NotNull String key, byte[] value);

        @NotNull
        Builder withHeader(@NotNull String key, @NotNull String value);

        @NotNull
        Event build();
    }
}
