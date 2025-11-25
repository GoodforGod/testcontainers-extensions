package io.goodforgod.testcontainers.extensions.nats;

import io.nats.client.Message;
import io.nats.client.impl.Headers;
import io.nats.client.support.Status;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Internal
final class ReceivedEventImpl implements ReceivedEvent {

    private final Value value;
    private final List<Header> headers;

    private final Message message;
    private final Status status;
    private final String subject;

    public ReceivedEventImpl(Message message) {
        this(new EventImpl.ValueImpl(message.getData()),
                getHeaders(message.getHeaders()),
                message,
                message.getSubject(),
                message.getStatus());
    }

    ReceivedEventImpl(Value value, List<Header> headers, Message message, String subject, Status status) {
        this.value = value;
        this.headers = headers;
        this.message = message;
        this.subject = subject;
        this.status = status;
    }

    @Override
    public @NotNull String sid() {
        return message.getSID();
    }

    @Override
    public @NotNull Message message() {
        return message;
    }

    @Override
    public @NotNull Status status() {
        return status;
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
    public @NotNull String subject() {
        return subject;
    }

    @Override
    public @Nullable String getReplyTo() {
        return message.getReplyTo();
    }

    private static List<Header> getHeaders(@Nullable Headers messageHeaders) {
        if (messageHeaders == null) {
            return List.of();
        }

        final List<Header> headers = new ArrayList<>();
        for (var header : messageHeaders.entrySet()) {
            for (String headerValue : header.getValue()) {
                headers.add(new EventImpl.HeaderImpl(header.getKey(),
                        new EventImpl.ValueImpl(headerValue.getBytes(StandardCharsets.UTF_8))));
            }
        }
        return List.copyOf(headers);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ReceivedEventImpl that = (ReceivedEventImpl) o;
        return Objects.equals(value, that.value)
                && Objects.equals(headers, that.headers)
                && Objects.equals(message.getSID(), that.message.getSID())
                && Objects.equals(subject, that.subject);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, headers, message.getSID(), subject);
    }

    @Override
    public String toString() {
        if (headers.isEmpty()) {
            return "[subject=" + subject +
                    ", value=" + value +
                    ']';
        } else {
            return "[subject=" + subject +
                    ", value=" + value +
                    ", headers=" + headers +
                    ']';
        }
    }
}
