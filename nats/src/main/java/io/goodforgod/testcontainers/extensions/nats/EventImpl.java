package io.goodforgod.testcontainers.extensions.nats;

import java.nio.charset.StandardCharsets;
import java.util.*;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

@Internal
final class EventImpl implements Event {

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
            return (value == null)
                    ? ""
                    : new String(value, StandardCharsets.UTF_8);
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
            ValueImpl value1 = (ValueImpl) o;
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

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            HeaderImpl header = (HeaderImpl) o;
            return Objects.equals(key, header.key) && Objects.equals(value, header.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(key, value);
        }

        @Override
        public String toString() {
            return "[key=" + key + ", value=" + value + ']';
        }
    }

    private final Value value;
    private final List<Header> headers;

    EventImpl(Value value, List<Header> headers) {
        this.value = value;
        this.headers = (headers == null)
                ? Collections.emptyList()
                : headers;
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

        private byte[] value;
        private final List<Header> headers = new ArrayList<>();

        @Override
        public @NotNull Builder withValue(byte[] value) {
            this.value = value;
            return this;
        }

        @Override
        public @NotNull Builder withValue(@NotNull String value) {
            return withValue(value.getBytes(StandardCharsets.UTF_8));
        }

        @Override
        public @NotNull Builder withValue(@NotNull JSONObject value) {
            return withValue(value.toString().getBytes(StandardCharsets.UTF_8));
        }

        @Override
        public @NotNull Builder withValue(@NotNull JSONArray value) {
            return withValue(value.toString().getBytes(StandardCharsets.UTF_8));
        }

        @Override
        public @NotNull Builder withHeader(@NotNull String key, byte[] value) {
            this.headers.add(new HeaderImpl(key, new ValueImpl(value)));
            return this;
        }

        @Override
        public @NotNull Builder withHeader(@NotNull String key, @NotNull String value) {
            return withHeader(key, value.getBytes(StandardCharsets.UTF_8));
        }

        @Override
        public @NotNull Event build() {
            return new EventImpl(new ValueImpl(value), List.copyOf(headers));
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        EventImpl event = (EventImpl) o;
        return Objects.equals(value, event.value) && Objects.equals(headers, event.headers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, headers);
    }

    @Override
    public String toString() {
        if (headers.isEmpty()) {
            return "[value=" + value + ']';
        } else {
            return "[value=" + value + ", headers=" + headers + ']';
        }
    }
}
