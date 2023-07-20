package io.goodforgod.testcontainers.extensions.kafka;

import io.goodforgod.testcontainers.extensions.kafka.KafkaConnection.Consumer;
import java.time.OffsetDateTime;
import org.jetbrains.annotations.NotNull;

/**
 * Kafka Event received from {@link Consumer}
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
     * @return The position of this record in the corresponding Kafka partition.
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
