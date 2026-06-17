package io.goodforgod.testcontainers.extensions.rabbitmq;

import com.rabbitmq.client.Delivery;
import org.jetbrains.annotations.NotNull;

/**
 * RabbitMQ Event received from {@link RabbitMQConnection.Consumer}
 */
public interface ReceivedEvent extends Event {

    @NotNull
    String queue();

    @NotNull
    String exchange();

    @NotNull
    String routingKey();

    long deliveryTag();

    boolean redelivered();

    @NotNull
    Delivery delivery();
}