package io.goodforgod.testcontainers.extensions.rabbitmq;

import com.rabbitmq.client.Delivery;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;

@Internal
final class ReceivedEventImpl implements ReceivedEvent {

    private final String queue;
    private final Delivery delivery;
    private final Value value;
    private final List<Header> headers;

    ReceivedEventImpl(String queue, Delivery delivery) {
        this.queue = queue;
        this.delivery = delivery;
        this.value = new EventImpl.ValueImpl(delivery.getBody());
        this.headers = getHeaders(delivery);
    }

    private static List<Header> getHeaders(Delivery delivery) {
        final Map<String, Object> headersRaw = Optional.ofNullable(delivery.getProperties())
                .map(com.rabbitmq.client.AMQP.BasicProperties::getHeaders)
                .orElse(Collections.emptyMap());
        final List<Header> headers = new ArrayList<>();
        headersRaw.forEach((k, v) -> {
            byte[] bytes = (v instanceof byte[] data)
                    ? data
                    : String.valueOf(v).getBytes(StandardCharsets.UTF_8);
            headers.add(new EventImpl.HeaderImpl(k, new EventImpl.ValueImpl(bytes)));
        });
        return List.copyOf(headers);
    }

    @Override
    public @NotNull String queue() {
        return queue;
    }

    @Override
    public @NotNull String exchange() {
        return delivery.getEnvelope().getExchange();
    }

    @Override
    public @NotNull String routingKey() {
        return delivery.getEnvelope().getRoutingKey();
    }

    @Override
    public long deliveryTag() {
        return delivery.getEnvelope().getDeliveryTag();
    }

    @Override
    public boolean redelivered() {
        return delivery.getEnvelope().isRedeliver();
    }

    @Override
    public @NotNull Delivery delivery() {
        return delivery;
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
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ReceivedEventImpl that = (ReceivedEventImpl) o;
        return deliveryTag() == that.deliveryTag() && redelivered() == that.redelivered()
                && Objects.equals(queue, that.queue) && Objects.equals(exchange(), that.exchange())
                && Objects.equals(routingKey(), that.routingKey()) && Objects.equals(value, that.value)
                && Objects.equals(headers, that.headers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(queue, exchange(), routingKey(), deliveryTag(), redelivered(), value, headers);
    }

    @Override
    public String toString() {
        return headers.isEmpty()
                ? "[queue=" + queue + ", exchange=" + exchange() + ", routingKey=" + routingKey() + ", deliveryTag="
                        + deliveryTag() + ", value=" + value + ']'
                : "[queue=" + queue + ", exchange=" + exchange() + ", routingKey=" + routingKey() + ", deliveryTag="
                        + deliveryTag() + ", value=" + value + ", headers=" + headers + ']';
    }
}
