package io.goodforgod.testcontainers.extensions.redpanda;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;

@Internal
final class ReceivedEventImpl implements ReceivedEvent {

    private final Key key;
    private final Value value;
    private final List<Header> headers;

    private final String topic;
    private final int partition;
    private final long offset;
    private final long timestamp;

    public ReceivedEventImpl(ConsumerRecord<byte[], byte[]> record) {
        this((record.key() == null)
                ? null
                : new EventImpl.KeyImpl(record.key()),
                new EventImpl.ValueImpl(record.value()),
                getHeaders(record),
                record.topic(),
                record.partition(),
                record.offset(),
                record.timestamp());
    }

    private static List<Header> getHeaders(ConsumerRecord<byte[], byte[]> record) {
        final List<Header> headers = new ArrayList<>();
        for (var header : record.headers()) {
            headers.add(new EventImpl.HeaderImpl(header.key(), new EventImpl.ValueImpl(header.value())));
        }
        return List.copyOf(headers);
    }

    ReceivedEventImpl(Key key, Value value, List<Header> headers, String topic, int partition, long offset, long timestamp) {
        this.key = key;
        this.value = value;
        this.headers = headers;
        this.topic = topic;
        this.partition = partition;
        this.offset = offset;
        this.timestamp = timestamp;
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

    @Override
    public @NotNull String topic() {
        return topic;
    }

    @Override
    public int partition() {
        return partition;
    }

    @Override
    public long offset() {
        return offset;
    }

    @Override
    public long timestamp() {
        return timestamp;
    }

    @Override
    public @NotNull OffsetDateTime datetime() {
        return OffsetDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.of("Z"));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ReceivedEventImpl event = (ReceivedEventImpl) o;
        return partition == event.partition && offset == event.offset && timestamp == event.timestamp
                && Objects.equals(key, event.key) && Objects.equals(value, event.value) && Objects.equals(headers, event.headers)
                && Objects.equals(topic, event.topic);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, value, headers, topic, partition, offset, timestamp);
    }

    @Override
    public String toString() {
        if (key == null && headers.isEmpty()) {
            return "[topic=" + topic +
                    ", offset=" + offset +
                    ", timestamp=" + datetime() +
                    ", value=" + value +
                    ", partition=" + partition +
                    ']';
        } else if (key == null) {
            return "[topic=" + topic +
                    ", offset=" + offset +
                    ", timestamp=" + datetime() +
                    ", value=" + value +
                    ", headers=" + headers +
                    ", partition=" + partition +
                    ']';
        } else if (headers.isEmpty()) {
            return "[topic=" + topic +
                    ", offset=" + offset +
                    ", timestamp=" + datetime() +
                    ", key=" + key +
                    ", value=" + value +
                    ", partition=" + partition +
                    ']';
        } else {
            return "[topic=" + topic +
                    ", offset=" + offset +
                    ", timestamp=" + datetime() +
                    ", key=" + key +
                    ", value=" + value +
                    ", headers=" + headers +
                    ", partition=" + partition +
                    ']';
        }
    }
}
