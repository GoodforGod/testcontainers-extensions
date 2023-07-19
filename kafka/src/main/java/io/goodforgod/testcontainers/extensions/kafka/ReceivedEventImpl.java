package io.goodforgod.testcontainers.extensions.kafka;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
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
        this(
                new EventImpl.KeyImpl(record.key()),
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
        return headers;
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
}
