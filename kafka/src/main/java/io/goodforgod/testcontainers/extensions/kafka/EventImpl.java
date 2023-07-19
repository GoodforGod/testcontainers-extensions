package io.goodforgod.testcontainers.extensions.kafka;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

@Internal
final class EventImpl implements Event {

    static final class KeyImpl implements Key {

        private final byte[] key;

        KeyImpl(byte[] key) {
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
    }

    static final class ValueImpl implements Value {

        private final byte[] value;

        ValueImpl(byte[] value) {
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
    }

    static final class HeaderImpl implements Header {

        private final String key;
        private final Value value;

        HeaderImpl(String key, Value value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public @NotNull String key() {
            return key;
        }

        @Override
        public @NotNull Value value() {
            return value;
        }
    }

    private final Key key;
    private final Value value;
    private final List<Header> headers;

    EventImpl(Key key, Value value, List<Header> headers) {
        this.key = key;
        this.value = value;
        this.headers = (headers == null)
                ? Collections.emptyList()
                : headers;
    }

    @Override
    public Key key() {
        return key;
    }

    @Override
    public @NotNull Value value() {
        return value;
    }

    @Override
    public @NotNull List<Header> headers() {
        return headers;
    }

    static final class EventBuilder implements Builder {

        private byte[] key;
        private byte[] value;
        private final List<Header> headers = new ArrayList<>();

        @Override
        public @NotNull Builder withKey(@NotNull String key) {
            this.key = key.getBytes(StandardCharsets.UTF_8);
            return this;
        }

        @Override
        public @NotNull Builder withKey(byte[] key) {
            this.key = key;
            return this;
        }

        @Override
        public @NotNull Builder withValue(byte[] value) {
            this.value = value;
            return this;
        }

        @Override
        public @NotNull Builder withValue(@NotNull String value) {
            this.value = value.getBytes(StandardCharsets.UTF_8);
            return this;
        }

        @Override
        public @NotNull Builder withValue(@NotNull JSONObject value) {
            this.value = value.toString().getBytes(StandardCharsets.UTF_8);
            return this;
        }

        @Override
        public @NotNull Builder withValue(@NotNull JSONArray value) {
            this.value = value.toString().getBytes(StandardCharsets.UTF_8);
            return this;
        }

        @Override
        public @NotNull Builder withHeader(@NotNull String key, byte[] value) {
            this.headers.add(new HeaderImpl(key, new ValueImpl(value)));
            return this;
        }

        @Override
        public @NotNull Builder withHeader(@NotNull String key, @NotNull String value) {
            this.headers.add(new HeaderImpl(key, new ValueImpl(value.getBytes(StandardCharsets.UTF_8))));
            return this;
        }

        @Override
        public @NotNull Event build() {
            return new EventImpl(new KeyImpl(key), new ValueImpl(value), List.copyOf(headers));
        }
    }
}
