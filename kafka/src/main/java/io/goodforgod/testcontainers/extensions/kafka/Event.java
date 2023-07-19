package io.goodforgod.testcontainers.extensions.kafka;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

public interface Event {

    interface Key {

        byte[] asBytes();

        @NotNull
        String asString();
    }

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

    Key key();

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
        return new EventImpl(null, new EventImpl.ValueImpl(valueAsBytes), Collections.emptyList());
    }

    @NotNull
    static Event ofValue(@NotNull String valueAsBytes) {
        return ofValue(valueAsBytes.getBytes(StandardCharsets.UTF_8));
    }

    @NotNull
    static Event ofValue(@NotNull JSONObject valueAsBytes) {
        return ofValue(valueAsBytes.toString().getBytes(StandardCharsets.UTF_8));
    }

    @NotNull
    static Event ofValue(@NotNull JSONArray valueAsBytes) {
        return ofValue(valueAsBytes.toString().getBytes(StandardCharsets.UTF_8));
    }

    @NotNull
    static Event ofValueAndRandomKey(byte[] valueAsBytes) {
        return new EventImpl(new EventImpl.KeyImpl(UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8)),
                new EventImpl.ValueImpl(valueAsBytes),
                Collections.emptyList());
    }

    @NotNull
    static Event ofValueAndRandomKey(@NotNull String valueAsBytes) {
        return ofValueAndRandomKey(valueAsBytes.getBytes(StandardCharsets.UTF_8));
    }

    @NotNull
    static Event ofValueAndRandomKey(@NotNull JSONObject valueAsBytes) {
        return ofValueAndRandomKey(valueAsBytes.toString().getBytes(StandardCharsets.UTF_8));
    }

    @NotNull
    static Event ofValueAndRandomKey(@NotNull JSONArray valueAsBytes) {
        return ofValueAndRandomKey(valueAsBytes.toString().getBytes(StandardCharsets.UTF_8));
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
