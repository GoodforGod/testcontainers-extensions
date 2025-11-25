package io.goodforgod.testcontainers.extensions.nats;

import io.nats.client.Message;
import io.nats.client.support.Status;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Nats Event received from {@link NatsConnection.Consumer}
 */
public interface ReceivedEvent extends Event {

    @NotNull
    String sid();

    @NotNull
    Message message();

    @NotNull
    Status status();

    /**
     * @return The topic this record is received from (never null)
     */
    @NotNull
    String subject();

    @Nullable
    String getReplyTo();
}
