package io.goodforgod.testcontainers.extensions.redpanda;

import io.goodforgod.testcontainers.extensions.redpanda.RedpandaConnection.Consumer;
import java.time.OffsetDateTime;
import org.jetbrains.annotations.NotNull;

/**
 * Redpanda Event received from {@link Consumer}
 */
public interface ReceivedEvent extends Event {

    /**
     * @return The topic this record is received from (never null)
     */
    @NotNull
    String topic();

    /**
     * @return The partition from which this record is received
     */
    int partition();

    /**
     * @return The position of this record in the corresponding Redpanda partition.
     */
    long offset();

    /**
     * @return The timestamp of this record
     */
    long timestamp();

    /**
     * @return The timestamp of this record as {@link OffsetDateTime}
     */
    @NotNull
    OffsetDateTime datetime();
}
